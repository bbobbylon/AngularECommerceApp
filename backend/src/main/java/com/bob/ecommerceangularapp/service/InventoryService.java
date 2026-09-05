package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.InventoryAdjustmentRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ProductVariantRepository;
import com.bob.ecommerceangularapp.dto.CsvImportResult;
import com.bob.ecommerceangularapp.dto.InventoryAdjustmentView;
import com.bob.ecommerceangularapp.dto.InventoryItemView;
import com.bob.ecommerceangularapp.entity.InventoryAdjustment;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SKU-level inventory management for the admin back-office (roadmap #15): a unified view across
 * {@link Product} (products with no variants — the product's own SKU is authoritative) and
 * {@link ProductVariant} (products sold per-variant — each variant SKU is authoritative; see
 * {@link ProductVariantService} for why the base product's stock is ignored once it has variants),
 * plus a CSV export/import for bulk restocks and an audit trail of every change.
 */
@Service
public class InventoryService {

    /** Mirrors {@link AdminService}'s dashboard threshold so the two "low stock" signals agree. */
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_CSV_IMPORT = "CSV_IMPORT";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final StockNotificationService stockNotificationService;

    public InventoryService(ProductRepository productRepository,
                            ProductVariantRepository variantRepository,
                            InventoryAdjustmentRepository adjustmentRepository,
                            StockNotificationService stockNotificationService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.stockNotificationService = stockNotificationService;
    }

    // ---------- read ----------

    @Transactional(readOnly = true)
    public List<InventoryItemView> list() {
        List<Product> products = productRepository.findAll();
        Map<Long, List<ProductVariant>> variantsByProduct = variantRepository.findAll().stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        List<InventoryItemView> items = new ArrayList<>();
        for (Product product : products) {
            List<ProductVariant> variants = variantsByProduct.get(product.getId());
            if (variants == null || variants.isEmpty()) {
                items.add(toView(product));
            } else {
                variants.stream()
                        .sorted(Comparator.comparingInt(ProductVariant::getSortOrder).thenComparing(ProductVariant::getId))
                        .forEach(v -> items.add(toView(v, product)));
            }
        }
        items.sort(Comparator.comparing(InventoryItemView::productName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(i -> i.variantLabel() == null ? "" : i.variantLabel()));
        return items;
    }

    @Transactional(readOnly = true)
    public Page<InventoryAdjustmentView> history(Pageable pageable) {
        return adjustmentRepository.findAllByOrderByDateCreatedDesc(pageable).map(InventoryService::toAdjustmentView);
    }

    private InventoryItemView toView(Product product) {
        int stock = product.getUnitsInStock();
        return new InventoryItemView(product.getSku(), product.getId(), product.getName(), null,
                stock, stock < LOW_STOCK_THRESHOLD, product.isActive());
    }

    private InventoryItemView toView(ProductVariant variant, Product product) {
        int stock = variant.getUnitsInStock();
        return new InventoryItemView(variant.getSku(), product.getId(), product.getName(), variant.label(),
                stock, stock < LOW_STOCK_THRESHOLD, variant.isActive());
    }

    private static InventoryAdjustmentView toAdjustmentView(InventoryAdjustment a) {
        return new InventoryAdjustmentView(a.getId(), a.getSku(), a.getProductName(), a.getPreviousQuantity(),
                a.getNewQuantity(), a.getDelta(), a.getSource(), a.getNote(), a.getDateCreated());
    }

    // ---------- write ----------

    /** Single manual stock edit (the inline "save" on the admin Inventory row). */
    @Transactional
    public InventoryItemView adjust(String sku, int newQuantity, String note) {
        return apply(sku, newQuantity, note, SOURCE_MANUAL)
                .orElseThrow(() -> new IllegalArgumentException("No product or variant with SKU: " + sku));
    }

    /**
     * Bulk stock update from an uploaded CSV. Expects a header row naming {@code sku} and
     * {@code quantity} columns (any order, case-insensitive extra columns ignored) — matches the
     * shape produced by {@link #exportCsv()}. Unknown SKUs and unparsable quantities are collected as
     * per-row errors rather than aborting the whole batch.
     */
    @Transactional
    public CsvImportResult importCsv(InputStream input) {
        List<String[]> rows;
        Map<String, Integer> columns;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("The CSV file is empty.");
            }
            columns = indexColumns(splitCsvLine(headerLine));
            if (!columns.containsKey("sku") || !columns.containsKey("quantity")) {
                throw new IllegalArgumentException("The CSV must have \"sku\" and \"quantity\" columns.");
            }
            rows = reader.lines().filter(line -> !line.isBlank()).map(InventoryService::splitCsvLine).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded CSV file.", e);
        }

        int skuCol = columns.get("sku");
        int qtyCol = columns.get("quantity");
        int noteCol = columns.getOrDefault("note", -1);

        int updated = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // header is row 1
            String[] row = rows.get(i);
            String sku = field(row, skuCol);
            if (sku == null || sku.isBlank()) {
                errors.add("Row " + rowNumber + ": missing SKU");
                continue;
            }
            String rawQty = field(row, qtyCol);
            Integer quantity = parseQuantity(rawQty);
            if (quantity == null) {
                errors.add("Row " + rowNumber + " (" + sku + "): invalid quantity \"" + rawQty + "\"");
                continue;
            }
            String note = noteCol >= 0 ? field(row, noteCol) : null;
            Optional<InventoryItemView> result = apply(sku.trim(), quantity, note, SOURCE_CSV_IMPORT);
            if (result.isEmpty()) {
                errors.add("Row " + rowNumber + ": no product or variant with SKU \"" + sku.trim() + "\"");
                continue;
            }
            updated++;
        }
        return new CsvImportResult(updated, errors);
    }

    /** CSV export of the current inventory snapshot — the same shape {@link #importCsv} expects back. */
    @Transactional(readOnly = true)
    public String exportCsv() {
        StringBuilder csv = new StringBuilder("sku,product,variant,quantity,active\n");
        for (InventoryItemView item : list()) {
            csv.append(csvField(item.sku())).append(',')
                    .append(csvField(item.productName())).append(',')
                    .append(csvField(item.variantLabel())).append(',')
                    .append(item.unitsInStock()).append(',')
                    .append(item.active()).append('\n');
        }
        return csv.toString();
    }

    /** Looks up the SKU as a product first, then a variant; records the adjustment either way. */
    private Optional<InventoryItemView> apply(String sku, int newQuantity, String note, String source) {
        int quantity = Math.max(0, newQuantity);

        Optional<Product> product = productRepository.findBySku(sku);
        if (product.isPresent()) {
            Product p = product.get();
            int previous = p.getUnitsInStock();
            p.setUnitsInStock(quantity);
            productRepository.save(p);
            log(sku, p.getName(), previous, quantity, source, note);
            if (quantity > 0) {
                stockNotificationService.notifyProductRestocked(p);
            }
            return Optional.of(toView(p));
        }

        Optional<ProductVariant> variant = variantRepository.findBySku(sku);
        if (variant.isPresent()) {
            ProductVariant v = variant.get();
            Product owner = v.getProduct();
            int previous = v.getUnitsInStock();
            v.setUnitsInStock(quantity);
            variantRepository.save(v);
            log(sku, owner.getName(), previous, quantity, source, note);
            if (quantity > 0) {
                stockNotificationService.notifyVariantRestocked(sku, owner);
            }
            return Optional.of(toView(v, owner));
        }

        return Optional.empty();
    }

    private void log(String sku, String productName, int previous, int newQuantity, String source, String note) {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setSku(sku);
        adjustment.setProductName(productName);
        adjustment.setPreviousQuantity(previous);
        adjustment.setNewQuantity(newQuantity);
        adjustment.setDelta(newQuantity - previous);
        adjustment.setSource(source);
        adjustment.setNote(note == null || note.isBlank() ? null : note.trim());
        adjustmentRepository.save(adjustment);
    }

    private static Integer parseQuantity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, Integer> indexColumns(String[] header) {
        Map<String, Integer> columns = new java.util.HashMap<>();
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].trim().toLowerCase(), i);
        }
        return columns;
    }

    private static String field(String[] row, int index) {
        return index >= 0 && index < row.length ? row[index] : null;
    }

    /** Minimal RFC 4180-ish split: handles double-quoted fields (with "" as an escaped quote). */
    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private static String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
