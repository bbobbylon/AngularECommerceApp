package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ProductVariantRepository;
import com.bob.ecommerceangularapp.dto.ProductVariantView;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductVariant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for variant price resolution and the checkout stock decrement. */
class ProductVariantServiceTest {

    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductVariantService service = new ProductVariantService(variantRepository, productRepository);

    @Test
    void viewsForProduct_resolvesPriceAndStockFlag() {
        Product product = Product.builder().id(1L).unitPrice(new BigDecimal("20.00"))
                .imageUrl("p.png").build();
        ProductVariant inherits = variant(10L, "SKU-A", null, 5);     // inherits product price
        ProductVariant overrides = variant(11L, "SKU-B", new BigDecimal("24.00"), 0); // own price, OOS

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(inherits, overrides));

        List<ProductVariantView> views = service.viewsForProduct(1L);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).unitPrice()).isEqualByComparingTo("20.00"); // inherited
        assertThat(views.get(0).inStock()).isTrue();
        assertThat(views.get(1).unitPrice()).isEqualByComparingTo("24.00"); // overridden
        assertThat(views.get(1).inStock()).isFalse();                       // 0 in stock
    }

    @Test
    void decrementForOrderItems_drawsDownVariantStockClampedAtZero() {
        ProductVariant v = variant(10L, "SKU-A", null, 3);
        when(variantRepository.findBySku("SKU-A")).thenReturn(Optional.of(v));

        service.decrementForOrderItems(List.of(orderItem("SKU-A", 5))); // more than in stock

        assertThat(v.getUnitsInStock()).isZero(); // clamped, not negative
        verify(variantRepository).save(v);
    }

    @Test
    void decrementForOrderItems_ignoresLinesWithoutAVariant() {
        service.decrementForOrderItems(List.of(orderItem(null, 2)));
        verify(variantRepository, never()).findBySku(any());
        verify(variantRepository, never()).save(any());
    }

    private ProductVariant variant(Long id, String sku, BigDecimal price, int stock) {
        return ProductVariant.builder().id(id).sku(sku).unitPrice(price)
                .unitsInStock(stock).active(true).build();
    }

    private OrderItem orderItem(String variantSku, int qty) {
        OrderItem item = new OrderItem();
        item.setQuantity(qty);
        item.setVariantSku(variantSku);
        return item;
    }
}
