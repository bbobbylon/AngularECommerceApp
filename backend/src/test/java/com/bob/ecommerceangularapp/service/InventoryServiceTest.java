package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.InventoryAdjustmentRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ProductVariantRepository;
import com.bob.ecommerceangularapp.dto.CsvImportResult;
import com.bob.ecommerceangularapp.dto.InventoryItemView;
import com.bob.ecommerceangularapp.entity.InventoryAdjustment;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductVariant;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for the merged product+variant inventory view, edits and CSV import. */
class InventoryServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final InventoryAdjustmentRepository adjustmentRepository = mock(InventoryAdjustmentRepository.class);
    private final StockNotificationService stockNotificationService = mock(StockNotificationService.class);
    private final InventoryService service = new InventoryService(
            productRepository, variantRepository, adjustmentRepository, stockNotificationService);

    private Product product(long id, String sku, String name, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setUnitsInStock(stock);
        p.setActive(true);
        return p;
    }

    private ProductVariant variant(long id, Product owner, String sku, String size, int stock) {
        ProductVariant v = new ProductVariant();
        v.setId(id);
        v.setProduct(owner);
        v.setSku(sku);
        v.setSize(size);
        v.setUnitsInStock(stock);
        v.setActive(true);
        return v;
    }

    @Test
    void list_showsSingleSkuProductsAndPerVariantLinesSeparately() {
        Product mug = product(1L, "MUG-1", "Mug", 3); // below LOW_STOCK_THRESHOLD (10)
        Product book = product(2L, "BOOK-1", "Book", 25);
        Product pad = product(3L, "PAD-1", "Pad", 999); // has variants -> its own row is skipped
        ProductVariant padSmall = variant(10L, pad, "PAD-1-S", "S", 5);
        ProductVariant padLarge = variant(11L, pad, "PAD-1-L", "L", 20);

        when(productRepository.findAll()).thenReturn(List.of(mug, book, pad));
        when(variantRepository.findAll()).thenReturn(List.of(padSmall, padLarge));

        List<InventoryItemView> items = service.list();

        assertThat(items).extracting(InventoryItemView::sku)
                .containsExactlyInAnyOrder("MUG-1", "BOOK-1", "PAD-1-S", "PAD-1-L");
        assertThat(items).filteredOn(i -> i.sku().equals("PAD-1")).isEmpty(); // base product row skipped
        assertThat(items).filteredOn(i -> i.sku().equals("MUG-1")).first()
                .satisfies(i -> assertThat(i.lowStock()).isTrue());
        assertThat(items).filteredOn(i -> i.sku().equals("BOOK-1")).first()
                .satisfies(i -> assertThat(i.lowStock()).isFalse());
        assertThat(items).filteredOn(i -> i.sku().equals("PAD-1-S")).first()
                .satisfies(i -> assertThat(i.variantLabel()).isEqualTo("S"));
    }

    @Test
    void adjust_updatesProductStockAndLogsHistory() {
        Product mug = product(1L, "MUG-1", "Mug", 0);
        when(productRepository.findBySku("MUG-1")).thenReturn(Optional.of(mug));

        InventoryItemView result = service.adjust("MUG-1", 15, "restock");

        assertThat(result.unitsInStock()).isEqualTo(15);
        assertThat(mug.getUnitsInStock()).isEqualTo(15);
        verify(productRepository).save(mug);
        verify(adjustmentRepository).save(any(InventoryAdjustment.class));
        verify(stockNotificationService).notifyProductRestocked(mug); // 0 -> 15 crosses back into stock
    }

    @Test
    void adjust_updatesVariantStockWithoutNotifyingWhenStillZero() {
        Product pad = product(3L, "PAD-1", "Pad", 999);
        ProductVariant padSmall = variant(10L, pad, "PAD-1-S", "S", 5);
        when(productRepository.findBySku("PAD-1-S")).thenReturn(Optional.empty());
        when(variantRepository.findBySku("PAD-1-S")).thenReturn(Optional.of(padSmall));

        InventoryItemView result = service.adjust("PAD-1-S", 0, null);

        assertThat(result.unitsInStock()).isZero();
        assertThat(padSmall.getUnitsInStock()).isZero();
        verify(variantRepository).save(padSmall);
        verify(stockNotificationService, never()).notifyVariantRestocked(any(), any());
    }

    @Test
    void adjust_rejectsUnknownSku() {
        when(productRepository.findBySku("NOPE")).thenReturn(Optional.empty());
        when(variantRepository.findBySku("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjust("NOPE", 5, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importCsv_appliesKnownRowsAndCollectsErrorsForBadOnes() {
        Product mug = product(1L, "MUG-1", "Mug", 3);
        when(productRepository.findBySku("MUG-1")).thenReturn(Optional.of(mug));
        when(productRepository.findBySku("UNKNOWN-SKU")).thenReturn(Optional.empty());
        when(variantRepository.findBySku("UNKNOWN-SKU")).thenReturn(Optional.empty());

        String csv = "sku,quantity,note\n"
                + "MUG-1,20,restock\n"
                + "UNKNOWN-SKU,5,\n"
                + "MUG-1,notanumber,\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = service.importCsv(input);

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().get(0)).contains("UNKNOWN-SKU");
        assertThat(result.errors().get(1)).contains("notanumber");
        assertThat(mug.getUnitsInStock()).isEqualTo(20);
        verify(adjustmentRepository, times(1)).save(any(InventoryAdjustment.class));
    }

    @Test
    void importCsv_rejectsMissingRequiredColumns() {
        String csv = "name,amount\nMUG-1,20\n";
        InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.importCsv(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sku");
    }

    @Test
    void exportCsv_includesHeaderAndEachInventoryLine() {
        Product mug = product(1L, "MUG-1", "Mug", 3);
        when(productRepository.findAll()).thenReturn(List.of(mug));
        when(variantRepository.findAll()).thenReturn(List.of());

        String csv = service.exportCsv();

        assertThat(csv).startsWith("sku,product,variant,quantity,active\n");
        assertThat(csv).contains("MUG-1,Mug,,3,true");
    }
}
