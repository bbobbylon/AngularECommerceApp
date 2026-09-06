package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.AdminProductRequest;
import com.bob.ecommerceangularapp.dto.AdminStats;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (mocked repositories) for the back-office logic: dashboard aggregation and the
 * product create/update normalisation (sale-price, image fallback, gallery cleanup).
 */
class AdminServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductCategoryRepository categoryRepository = mock(ProductCategoryRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final NewsletterSubscriberRepository subscriberRepository = mock(NewsletterSubscriberRepository.class);
    private final StockNotificationService stockNotificationService = mock(StockNotificationService.class);

    private final AdminService service = new AdminService(productRepository, categoryRepository,
            orderRepository, customerRepository, subscriberRepository, stockNotificationService);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void listProducts_scopesToCurrentTenant() {
        TenantContext.set(7L);

        service.listProducts(PageRequest.of(0, 10));

        verify(productRepository).findAllByTenantId(eq(7L), any());
    }

    @Test
    void createProduct_stampsCurrentTenantOntoTheNewProduct() {
        TenantContext.set(7L);
        when(categoryRepository.findByIdAndTenantId(eq(1L), eq(7L))).thenReturn(Optional.of(new ProductCategory("Books")));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        AdminProductRequest request = new AdminProductRequest(
                "SKU3", "Notebook", "desc", new BigDecimal("4.00"), null, "https://img/c.png", null, true, 2, 1L);

        Product saved = service.createProduct(request);

        assertThat(saved.getTenantId()).isEqualTo(7L);
    }

    @Test
    void stats_aggregatesRepositoryCounts() {
        when(productRepository.countByTenantId(any())).thenReturn(40L);
        when(productRepository.countByTenantIdAndActiveTrue(any())).thenReturn(35L);
        when(productRepository.countByTenantIdAndUnitsInStockLessThan(any(), anyInt())).thenReturn(4L);
        when(productRepository.countByTenantIdAndOriginalPriceNotNull(any())).thenReturn(9L);
        when(orderRepository.countByTenantId(any())).thenReturn(12L);
        when(orderRepository.sumTotalRevenue(any())).thenReturn(new BigDecimal("1234.56"));
        when(customerRepository.countByTenantId(any())).thenReturn(8L);
        when(customerRepository.countByTenantIdAndNewsletterSubscribedTrue(any())).thenReturn(5L);
        when(subscriberRepository.countBySubscribedTrue()).thenReturn(3L);

        AdminStats stats = service.stats();

        assertThat(stats.totalProducts()).isEqualTo(40L);
        assertThat(stats.activeProducts()).isEqualTo(35L);
        assertThat(stats.lowStockProducts()).isEqualTo(4L);
        assertThat(stats.productsOnSale()).isEqualTo(9L);
        assertThat(stats.totalOrders()).isEqualTo(12L);
        assertThat(stats.totalRevenue()).isEqualByComparingTo("1234.56");
        assertThat(stats.totalCustomers()).isEqualTo(8L);
        assertThat(stats.newsletterSubscribers()).isEqualTo(8L); // 5 customers + 3 standalone subscribers
    }

    @Test
    void createProduct_dropsSalePriceNotAboveUnitPrice_andFillsImageFallback() {
        ProductCategory category = new ProductCategory("Books");
        when(categoryRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // originalPrice == unitPrice -> not a real sale -> normalized to null; blank image -> fallback;
        // gallery has a blank and a duplicate -> cleaned.
        AdminProductRequest request = new AdminProductRequest(
                "SKU1", "Cool Mug", "desc", new BigDecimal("20.00"), new BigDecimal("20.00"),
                "  ", List.of("  ", "https://img/a.png", "https://img/a.png"), true, 5, 1L);

        Product saved = service.createProduct(request);

        assertThat(saved.getOriginalPrice()).isNull();
        assertThat(saved.getImageUrl()).contains("placehold.co").contains("Cool");
        assertThat(saved.getAdditionalImages()).containsExactly("https://img/a.png");
        assertThat(saved.getCategory()).isSameAs(category);
    }

    @Test
    void createProduct_keepsSalePriceAboveUnitPrice() {
        when(categoryRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(new ProductCategory("Books")));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminProductRequest request = new AdminProductRequest(
                "SKU2", "Book", "desc", new BigDecimal("15.00"), new BigDecimal("25.00"),
                "https://img/b.png", null, true, 3, 1L);

        Product saved = service.createProduct(request);

        assertThat(saved.getOriginalPrice()).isEqualByComparingTo("25.00");
        assertThat(saved.getImageUrl()).isEqualTo("https://img/b.png");
        assertThat(saved.getAdditionalImages()).isEmpty();
    }

    @Test
    void updateProduct_throwsWhenMissing() {
        when(productRepository.findByIdAndTenantId(eq(99L), any())).thenReturn(Optional.empty());
        AdminProductRequest request = new AdminProductRequest(
                "S", "N", null, new BigDecimal("1.00"), null, "x", null, true, 1, 1L);

        assertThatThrownBy(() -> service.updateProduct(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }
}
