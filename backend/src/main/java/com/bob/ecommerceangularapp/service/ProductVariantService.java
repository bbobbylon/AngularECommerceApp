package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ProductVariantRepository;
import com.bob.ecommerceangularapp.dto.AdminVariantRequest;
import com.bob.ecommerceangularapp.dto.ProductVariantView;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductVariant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Owns product-variant reads (storefront), writes (admin upsert) and the checkout stock decrement.
 * Variant stock is the authoritative SKU-level inventory; the legacy product-level
 * {@code unitsInStock} stays a display figure (it was never decremented and still isn't).
 */
@Service
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final StockNotificationService stockNotificationService;

    public ProductVariantService(ProductVariantRepository variantRepository,
                                 ProductRepository productRepository,
                                 StockNotificationService stockNotificationService) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.stockNotificationService = stockNotificationService;
    }

    // ---------- storefront reads ----------

    /** Active variants for a product, resolved (price/image filled in) for direct rendering. */
    @Transactional(readOnly = true)
    public List<ProductVariantView> viewsForProduct(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return List.of();
        }
        return variantRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
                .filter(ProductVariant::isActive)
                .map(v -> toView(v, product))
                .toList();
    }

    private ProductVariantView toView(ProductVariant v, Product product) {
        BigDecimal price = v.getUnitPrice() != null ? v.getUnitPrice() : product.getUnitPrice();
        String image = (v.getImageUrl() != null && !v.getImageUrl().isBlank())
                ? v.getImageUrl() : product.getImageUrl();
        return new ProductVariantView(v.getId(), v.getSku(), v.getColor(), v.getSize(), v.label(),
                price, v.getUnitsInStock(), v.getUnitsInStock() > 0, image);
    }

    // ---------- admin ----------

    /** Full variant list for a product (admin editor), including inactive ones, in display order. */
    @Transactional(readOnly = true)
    public List<AdminVariantRequest> adminListForProduct(Long productId) {
        return variantRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
                .map(ProductVariantService::toRequest)
                .toList();
    }

    private static AdminVariantRequest toRequest(ProductVariant v) {
        return new AdminVariantRequest(v.getId(), v.getSku(), v.getColor(), v.getSize(),
                v.getUnitPrice(), v.getUnitsInStock(), v.getImageUrl(), v.getSortOrder(), v.isActive());
    }

    /**
     * Replaces a product's variants with the supplied set: existing ids are updated, new ones inserted,
     * and any current variant whose id isn't in the request is deleted. Idempotent — the admin form
     * always sends the complete desired list.
     */
    @Transactional
    public List<AdminVariantRequest> replaceVariants(Long productId, List<AdminVariantRequest> requests) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<ProductVariant> existing = variantRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        Map<Long, ProductVariant> byId = existing.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        List<ProductVariant> kept = (requests == null ? List.<AdminVariantRequest>of() : requests).stream()
                .map(req -> {
                    ProductVariant v = req.id() != null ? byId.remove(req.id()) : null;
                    if (v == null) {
                        v = new ProductVariant();
                        v.setProduct(product);
                    }
                    v.setSku(req.sku().trim());
                    v.setColor(blankToNull(req.color()));
                    v.setSize(blankToNull(req.size()));
                    v.setUnitPrice(req.unitPrice());
                    v.setUnitsInStock(Math.max(0, req.unitsInStock()));
                    v.setImageUrl(blankToNull(req.imageUrl()));
                    v.setSortOrder(req.sortOrder());
                    v.setActive(req.active());
                    return v;
                })
                .toList();

        // anything still left in byId was omitted from the request → delete it
        if (!byId.isEmpty()) {
            variantRepository.deleteAll(byId.values());
        }
        List<ProductVariant> saved = variantRepository.saveAll(kept);
        // Any variant that now has stock → notify back-in-stock subscribers waiting on that SKU (gated).
        for (ProductVariant v : saved) {
            if (v.getUnitsInStock() > 0) {
                stockNotificationService.notifyVariantRestocked(v.getSku(), product);
            }
        }
        return saved.stream().map(ProductVariantService::toRequest).toList();
    }

    // ---------- checkout ----------

    /**
     * Decrements variant stock for the order's lines that name a variant SKU (clamped at zero). Called
     * inside the checkout transaction so it commits atomically with the order.
     */
    @Transactional
    public void decrementForOrderItems(Collection<OrderItem> items) {
        if (items == null) {
            return;
        }
        for (OrderItem item : items) {
            String sku = item.getVariantSku();
            if (sku == null || sku.isBlank()) {
                continue;
            }
            variantRepository.findBySku(sku).ifPresent(v -> {
                int remaining = Math.max(0, v.getUnitsInStock() - Math.max(0, item.getQuantity()));
                v.setUnitsInStock(remaining);
                variantRepository.save(v);
            });
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
