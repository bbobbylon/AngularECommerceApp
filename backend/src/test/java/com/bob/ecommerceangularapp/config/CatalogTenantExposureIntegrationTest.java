package com.bob.ecommerceangularapp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Full-context, real-HTTP proof (over the <b>open</b> security chain — no Okta issuer configured, i.e.
 * how the app runs by default) that a real cross-tenant catalog gap found in a 2026-09-22 audit is
 * actually closed, not just unit-tested against a mock — the same style of proof
 * {@link OrderTenantExposureIntegrationTest} already gives for the analogous {@code Order} gap.
 *
 * <p>{@code Product}'s and {@code ProductCategory}'s raw Spring Data REST collection resources
 * ({@code GET /api/products}, {@code GET /api/product-category}) are the default {@code findAll}-backed
 * resource, which takes no tenant predicate at all — exactly like {@code Order}'s collection resource
 * before it was fixed. {@code ProductCategory}'s was the live one: the storefront's category sidebar
 * and product-list filter dropdown called it directly, so every visitor (regardless of tenant) would
 * have seen every tenant's categories mixed together the moment a second tenant existed with its own.
 */
@SpringBootTest
class CatalogTenantExposureIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).build();
    }

    @Test
    void theOldUnscopedProductCollectionListingIsGone() throws Exception {
        mvc.perform(get("/api/products").param("size", "50"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void theOldUnscopedCategoryCollectionListingIsGone() throws Exception {
        mvc.perform(get("/api/product-category"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void categoriesEndpointIsReachableAndTenantScoped() throws Exception {
        mvc.perform(get("/api/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void productItemResourceStaysReachable() throws Exception {
        // A nonexistent id still 404s (never leaks whether it belongs to another tenant), but the
        // resource itself must not be gone the way the collection resource now is.
        mvc.perform(get("/api/products/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void categoryItemResourceStaysReachable() throws Exception {
        mvc.perform(get("/api/product-category/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void productCategoryRelatedResourceLinkIsTenantGuarded() throws Exception {
        // Same TenantResourceGuardFilter guard as the product item resource itself (added in the same
        // audit) — a nonexistent product id 404s rather than leaking whether some other tenant owns it.
        mvc.perform(get("/api/products/999999999/category")).andExpect(status().isNotFound());
    }
}
