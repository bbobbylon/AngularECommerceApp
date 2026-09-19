package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ReviewRepository;
import com.bob.ecommerceangularapp.dto.ReviewRequest;
import com.bob.ecommerceangularapp.entity.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards against a real cross-tenant gap found during roadmap #21's tenant-scoping audit:
 * {@code create()} used to look up the product with a bare {@code findById}, so a caller on any
 * tenant could submit a review against another tenant's product id — writing a review row that mixed
 * the two tenants and silently polluting that other tenant's denormalized product rating.
 */
class ReviewServiceTest {

    private static final Long TENANT_ID = 7L;

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ReviewService service = new ReviewService(reviewRepository, productRepository);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void create_rejectsAProductIdBelongingToAnotherTenant() {
        // Nothing stubbed for findByIdAndTenantId(5L, TENANT_ID) — simulates product 5 existing, but
        // owned by a different tenant, exactly like Mockito's default empty Optional would if the real
        // repository ran the tenant-scoped query against a cross-tenant id.
        ReviewRequest request = new ReviewRequest(5L, "Reviewer", 5, "Great product");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_succeedsForAProductOwnedByTheCurrentTenant() {
        Product product = new Product();
        product.setId(5L);
        product.setTenantId(TENANT_ID);
        when(productRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findByProductId(5L)).thenReturn(java.util.List.of());

        var view = service.create(new ReviewRequest(5L, "Reviewer", 4, "Pretty good"));

        assertThat(view.rating()).isEqualTo(4);
        verify(reviewRepository).save(any());
    }
}
