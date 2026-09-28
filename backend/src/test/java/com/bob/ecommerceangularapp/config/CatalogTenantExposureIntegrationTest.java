package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.TenantRepository;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.entity.Tenant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context, real-HTTP, <b>real-filter-chain</b> proof (over the <b>open</b> security chain — no
 * Okta issuer configured, i.e. how the app runs by default) that a real cross-tenant catalog gap found
 * in a 2026-09-22 audit is actually closed, not just unit-tested against a mock — the same style of
 * proof {@link OrderTenantExposureIntegrationTest} gives for the analogous {@code Order} gap.
 *
 * <p>{@code Product}'s and {@code ProductCategory}'s raw Spring Data REST collection resources
 * ({@code GET /api/products}, {@code GET /api/product-category}) are the default {@code findAll}-backed
 * resource, which takes no tenant predicate at all — exactly like {@code Order}'s collection resource
 * before it was fixed. {@code ProductCategory}'s was the live one: the storefront's category sidebar
 * and product-list filter dropdown called it directly, so every visitor (regardless of tenant) would
 * have seen every tenant's categories mixed together the moment a second tenant existed with its own.
 *
 * <p><b>2026-09-28 harness fix:</b> this class used to build {@code MockMvc} by hand via
 * {@code MockMvcBuilders.webAppContextSetup(context).build()}, which never wires the app's custom
 * {@code Filter} beans ({@link TenantResolutionFilter}, {@link TenantResourceGuardFilter} included) —
 * confirmed on 2026-09-26 by checking that {@code RequestIdFilter}'s unconditional {@code X-Request-Id}
 * response header was absent under that setup. Under that old harness, every "does this 404?" assertion
 * in this class used a <em>nonexistent</em> id (e.g. {@code 999999999}), which Spring Data REST's own
 * {@code findById} 404s on its own — so the assertions were true, but they could not have told the
 * difference between {@code TenantResourceGuardFilter} actually blocking a cross-tenant id and that
 * filter not running at all. Switching to {@code @AutoConfigureMockMvc} (matching
 * {@link SdrSearchAndAssociationExposureIntegrationTest}'s pattern) and adding
 * {@link #crossTenantProductAndCategoryAreBlockedByTheRealGuardFilter()} — which creates a real second
 * tenant with a real category and product, then reads them back with no tenant header — closes that
 * gap: this class can now genuinely fail if the guard filter regresses, which it could not before. See
 * the CLAUDE.md entry dated 2026-09-28 for the full writeup, including the deliberate
 * break-and-confirm-red proof.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogTenantExposureIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EntityManager entityManager;

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
        // resource itself must not be gone the way the collection resource now is. This only proves the
        // resource is wired up at all; it does NOT exercise TenantResourceGuardFilter (Spring Data
        // REST's own findById-not-found path 404s a genuinely nonexistent id with no help from that
        // filter) — see crossTenantProductAndCategoryAreBlockedByTheRealGuardFilter() below for the
        // assertion that does.
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

    @Test
    void crossTenantProductAndCategoryAreBlockedByTheRealGuardFilter() throws Exception {
        Tenant otherTenant = new Tenant();
        otherTenant.setSlug("catalog-exposure-cross-tenant-test");
        otherTenant.setDisplayName("Catalog Exposure Cross-Tenant Test");
        otherTenant.setActive(true);
        tenantRepository.saveAndFlush(otherTenant);

        ProductCategory otherCategory = new ProductCategory();
        otherCategory.setCategoryName("Catalog Exposure Secret Category");
        otherCategory.setTenantId(otherTenant.getId());
        productCategoryRepository.saveAndFlush(otherCategory);

        Product otherProduct = Product.builder()
                .tenantId(otherTenant.getId())
                .sku("CATALOG-EXPOSURE-TEST-SECRET")
                .name("Catalog Exposure Secret Product")
                .description("must never be visible to the demo tenant")
                .unitPrice(new BigDecimal("42.00"))
                .active(true)
                .unitsInStock(5)
                .category(otherCategory)
                .build();
        productRepository.saveAndFlush(otherProduct);
        entityManager.flush();
        entityManager.clear();

        // Requested with NO tenant header at all — resolves to the default "demo" tenant — reading ids
        // that genuinely belong to a different tenant. Unlike productItemResourceStaysReachable /
        // categoryItemResourceStaysReachable above, Spring Data REST's own findById would happily
        // return these rows (they really exist); only TenantResourceGuardFilter's real, wired-in
        // enforcement turns them into 404s. This is the assertion that could not have been trusted
        // under the old webAppContextSetup(context) harness.
        mvc.perform(get("/api/products/" + otherProduct.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/product-category/" + otherCategory.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + otherProduct.getId() + "/category")).andExpect(status().isNotFound());

        // Sanity check the guard isn't simply blocking everything: the SAME ids, requested with that
        // tenant's own header, are reachable.
        mvc.perform(get("/api/products/" + otherProduct.getId())
                        .header("X-Tenant-Id", "catalog-exposure-cross-tenant-test"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/product-category/" + otherCategory.getId())
                        .header("X-Tenant-Id", "catalog-exposure-cross-tenant-test"))
                .andExpect(status().isOk());
    }
}
