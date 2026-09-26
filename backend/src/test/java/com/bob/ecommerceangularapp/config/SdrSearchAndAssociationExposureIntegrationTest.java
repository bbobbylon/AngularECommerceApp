package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.TenantRepository;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.entity.Tenant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context, real-HTTP, real-filter-chain proof (2026-09-26 follow-up audit — a continuation of the
 * 2026-09-18/09-19/09-22 sweeps) that a third round of unscoped Spring Data REST surfaces on the
 * already-partly-hardened {@code Product}/{@code ProductCategory}/{@code Order} repositories is closed.
 *
 * <p>Deliberately uses {@code @AutoConfigureMockMvc} rather than the manual
 * {@code MockMvcBuilders.webAppContextSetup(context)} pattern {@link OrderTenantExposureIntegrationTest}
 * and {@link CatalogTenantExposureIntegrationTest} use: only {@code @AutoConfigureMockMvc} wires the
 * app's registered {@code Filter} beans (notably {@code TenantResolutionFilter} /
 * {@code TenantResourceGuardFilter}) into MockMvc dispatch — confirmed by a throwaway probe in this
 * session that {@code webAppContextSetup(context).build()} with no {@code .addFilters(...)} never sets
 * the {@code X-Request-Id} response header {@code RequestIdFilter} unconditionally adds, i.e. none of
 * this app's custom filters run under that setup. That means the two tests named above can only prove
 * an SDR-exposure-level fix (a resource genuinely gone, which Spring Data REST enforces on its own) —
 * they cannot prove a {@code TenantResourceGuardFilter} regression, since the filter that would need to
 * catch it never executes in their harness. This class exists specifically to give the two genuinely
 * filter-dependent, cross-tenant-data proofs below the real thing to run against. See the CLAUDE.md
 * entry dated 2026-09-26 for the full writeup, including this test-harness finding as its own item.
 *
 * <p>Three genuinely new gaps were found and fixed this round, all confirmed unscoped/reachable before
 * the fix via a real filter-chain probe:
 * <ul>
 *   <li>{@code OrderRepository.sumTotalRevenue} — a {@code @Query} method exported as a public SDR
 *   search resource, binding its {@code tenantId} parameter straight from the caller's query string with
 *   no auth at all: {@code GET /api/orders/search/sumTotalRevenue?tenantId=<any tenant>} returned that
 *   tenant's real total revenue to an anonymous caller.</li>
 *   <li>{@code ProductRepository.countByActiveTrue}/{@code countByOriginalPriceNotNull}/
 *   {@code countByUnitsInStockLessThan} — exported with no {@code tenantId} parameter at all, so each
 *   returned a platform-wide aggregate mixing every tenant's catalog together.</li>
 *   <li>{@code ProductCategory.products} (the {@code @OneToMany} inverse of {@code Product.category}) —
 *   {@code @JsonIgnore} only suppresses it from the embedded category representation, not from Spring
 *   Data REST's separate related-resource endpoint. {@code TenantResourceGuardFilter} correctly 404s
 *   {@code GET /api/product-category/{id}} for a category id belonging to another tenant, but the
 *   sibling {@code GET /api/product-category/{id}/products} link for that exact id was never in the
 *   guard's pattern list and returned 200 with that other tenant's real product data — proven live in
 *   this session by creating a genuine second-tenant category + product and reading it back with no
 *   tenant header at all.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SdrSearchAndAssociationExposureIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Long demoTenantId;

    @BeforeEach
    void setUp() {
        demoTenantId = tenantRepository.findBySlugAndActiveTrue("demo").orElseThrow().getId();
    }

    @Test
    void sumTotalRevenueSearchResourceIsGone() throws Exception {
        mvc.perform(get("/api/orders/search/sumTotalRevenue").param("tenantId", String.valueOf(demoTenantId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderSearchResourceListingHasNoExposedLinksLeft() throws Exception {
        // Every OrderRepository derived/@Query method is now @RestResource(exported = false), so the
        // whole /search resource collection is gone, not just the one method.
        mvc.perform(get("/api/orders/search")).andExpect(status().isNotFound());
    }

    @Test
    void productCountAggregateSearchResourcesAreGone() throws Exception {
        mvc.perform(get("/api/products/search/countByActiveTrue")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/search/countByOriginalPriceNotNull")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/search/countByUnitsInStockLessThan").param("threshold", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void productSearchResourceListingHasNoExposedLinksLeft() throws Exception {
        mvc.perform(get("/api/products/search")).andExpect(status().isNotFound());
    }

    @Test
    void productCategoryProductsAssociationLinkIsGoneEvenForOwnTenantsData() throws Exception {
        // Category 1 (seeded for the demo tenant, with real products in it) previously returned a full,
        // real product list through this link even for same-tenant data — the association is fully
        // suppressed now, not merely tenant-filtered, since nothing legitimate ever called it (the
        // storefront already reads GET /api/catalog/categories instead).
        mvc.perform(get("/api/product-category/1/products")).andExpect(status().isNotFound());
    }

    @Test
    void productCategoryProductsAssociationLinkNeverLeaksAnotherTenantsRealProduct() throws Exception {
        Tenant otherTenant = new Tenant();
        otherTenant.setSlug("sdr-exposure-test-tenant");
        otherTenant.setDisplayName("SDR Exposure Test Tenant");
        otherTenant.setActive(true);
        tenantRepository.saveAndFlush(otherTenant);

        ProductCategory otherCategory = new ProductCategory();
        otherCategory.setCategoryName("Other Tenant Secret Category");
        otherCategory.setTenantId(otherTenant.getId());
        productCategoryRepository.saveAndFlush(otherCategory);

        Product otherProduct = Product.builder()
                .tenantId(otherTenant.getId())
                .sku("SDR-EXPOSURE-TEST-SECRET")
                .name("Other Tenant Secret Product")
                .description("must never be visible to the demo tenant")
                .unitPrice(new BigDecimal("1234.56"))
                .active(true)
                .unitsInStock(1)
                .category(otherCategory)
                .build();
        productRepository.saveAndFlush(otherProduct);
        entityManager.flush();
        entityManager.clear();

        // Requested with NO tenant header at all (resolves to the default "demo" tenant), reading a
        // category id that belongs to a different tenant entirely.
        mvc.perform(get("/api/product-category/" + otherCategory.getId() + "/products"))
                .andExpect(status().isNotFound());

        // Sanity check the sibling item-resource guard (TenantResourceGuardFilter) still 404s the
        // category itself the same way, so this test is exercising the real filter chain.
        mvc.perform(get("/api/product-category/" + otherCategory.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderItemResourceAndCrossTenantGuardStillWorkUnaffected() throws Exception {
        // Regression guard: none of the fixes above should have touched the item resources or the
        // guard filter's own behavior (roadmap #21) — same-tenant reachable, cross-tenant 404.
        Tenant otherTenant = new Tenant();
        otherTenant.setSlug("sdr-exposure-test-order-tenant");
        otherTenant.setDisplayName("SDR Exposure Order Test Tenant");
        otherTenant.setActive(true);
        tenantRepository.saveAndFlush(otherTenant);

        Order order = new Order();
        order.setOrderTrackingNumber("SDR-EXPOSURE-TEST-ORDER");
        order.setTotalQuantity(1);
        order.setTotalPrice(new BigDecimal("9.99"));
        order.setStatus("Received");
        order.setTenantId(demoTenantId);

        Address shipping = new Address();
        shipping.setStreet("1 Test Ln");
        shipping.setCity("Testville");
        shipping.setState("California");
        shipping.setCountry("United States");
        shipping.setZipCode("90210");
        order.setShippingAddress(shipping);

        Address billing = new Address();
        billing.setStreet("1 Test Ln");
        billing.setCity("Testville");
        billing.setState("California");
        billing.setCountry("United States");
        billing.setZipCode("90210");
        order.setBillingAddress(billing);

        OrderItem item = new OrderItem();
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("9.99"));
        item.setProductId(1L);
        item.setImageUrl("https://example.test/sdr-exposure.png");
        order.add(item);

        Order saved = orderRepository.saveAndFlush(order);

        mvc.perform(get("/api/orders/" + saved.getId())).andExpect(status().isOk());
        mvc.perform(get("/api/orders/" + saved.getId()).header("X-Tenant-Id", "sdr-exposure-test-order-tenant"))
                .andExpect(status().isNotFound());
    }
}
