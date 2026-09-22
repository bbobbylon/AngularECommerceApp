package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against a real cross-tenant leak found during a 2026-09-22 audit: the raw Spring Data REST
 * collection resource {@code GET /api/product-category} (no tenant predicate — Spring Data REST's
 * default {@code findAll}-backed collection resource can't take one) mixed every tenant's categories
 * into one response, and the storefront's own category sidebar / product-list filter dropdown called
 * it directly. {@code findAllByTenantId} is the tenant-scoped replacement now served at
 * {@code GET /api/catalog/categories} (see {@code ProductFilterController}, {@code MyDataRestConfig},
 * and {@code docs/SECURITY.md}); this proves the query itself never crosses tenants.
 */
@DataJpaTest
class ProductCategoryRepositoryTest {

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Test
    void findAllByTenantId_neverCrossesTenants() {
        save("Books", 1L);
        save("Coffee Mugs", 1L);
        save("Luggage", 2L);

        List<ProductCategory> forTenantOne = categoryRepository.findAllByTenantId(1L);
        List<ProductCategory> forTenantTwo = categoryRepository.findAllByTenantId(2L);

        assertThat(forTenantOne).extracting(ProductCategory::getCategoryName)
                .containsExactlyInAnyOrder("Books", "Coffee Mugs");
        assertThat(forTenantTwo).extracting(ProductCategory::getCategoryName)
                .containsExactly("Luggage");
    }

    @Test
    void findAllByTenantId_emptyForUnknownTenant() {
        save("Books", 1L);

        assertThat(categoryRepository.findAllByTenantId(99L)).isEmpty();
    }

    private void save(String name, Long tenantId) {
        ProductCategory category = new ProductCategory(name);
        category.setTenantId(tenantId);
        categoryRepository.save(category);
    }
}
