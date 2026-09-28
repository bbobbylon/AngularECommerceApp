package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.TenantRepository;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
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
 * Okta issuer configured, i.e. how the app runs by default) that the roadmap #21 order-history gap is
 * actually closed, not just unit-tested against a mock:
 * <ul>
 *   <li>the raw Spring Data REST search {@code /api/orders/search/findByCustomerEmailOrderByDateCreatedDesc}
 *   and the raw collection {@code GET /api/orders} — both previously reachable by anyone, with no
 *   tenant predicate, mixing every tenant's order history together — are gone;</li>
 *   <li>their tenant-scoped replacements ({@code AccountController}, {@code OrderHistoryController})
 *   are reachable and return the expected paged-JSON shape;</li>
 *   <li>a genuinely cross-tenant order id — one that really exists, just not for the caller's tenant —
 *   is blocked by {@code TenantResourceGuardFilter}, not merely absent from the database.</li>
 * </ul>
 * {@link com.bob.ecommerceangularapp.dao.OrderRepositoryTest} covers the tenant-isolation of the query
 * itself at the JPA layer (no HTTP, no filter chain); this class covers the same guarantee end to end,
 * through the real Spring MVC dispatch and the real registered {@link jakarta.servlet.Filter} beans.
 *
 * <p><b>2026-09-28 harness fix:</b> this class used to build {@code MockMvc} by hand via
 * {@code MockMvcBuilders.webAppContextSetup(context).build()}, which never wires the app's custom
 * {@code Filter} beans ({@link TenantResolutionFilter}, {@link TenantResourceGuardFilter} included) —
 * confirmed on 2026-09-26 by checking that {@code RequestIdFilter}'s unconditional {@code X-Request-Id}
 * response header was absent under that setup. Under that old harness, every "does this 404?" assertion
 * in this class used a <em>nonexistent</em> order id (e.g. {@code 999999999}), which Spring Data REST's
 * own {@code findById} 404s on its own — so the assertions were true, but they could not have told the
 * difference between {@code TenantResourceGuardFilter} actually blocking a cross-tenant id and that
 * filter not running at all. Switching to {@code @AutoConfigureMockMvc} (matching
 * {@link SdrSearchAndAssociationExposureIntegrationTest}'s pattern, the only technique that wires the
 * app's registered filters into MockMvc dispatch) and adding
 * {@link #crossTenantOrderIsBlockedByTheRealGuardFilter()} — which creates a real second tenant and a
 * real order for it, then reads that order back with no tenant header — closes that gap: this class can
 * now genuinely fail if the guard filter regresses, which it could not before. See the CLAUDE.md entry
 * dated 2026-09-28 for the full writeup, including the deliberate break-and-confirm-red proof.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderTenantExposureIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void theOldUnscopedEmailSearchIsGone() throws Exception {
        mvc.perform(get("/api/orders/search/findByCustomerEmailOrderByDateCreatedDesc")
                        .param("email", "anyone@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void theOldUnscopedCollectionListingIsGone() throws Exception {
        mvc.perform(get("/api/orders").param("sort", "dateCreated,desc").param("size", "50"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void theOrderItemResourceStaysReachable() throws Exception {
        // A nonexistent id still 404s (never leaks whether it belongs to another tenant), but the
        // resource itself must not be gone the way the collection/search resources now are —
        // checkout/API consumers still rely on GET /api/orders/{id}. This only proves the resource is
        // wired up at all; it does NOT exercise TenantResourceGuardFilter (Spring Data REST's own
        // findById-not-found path 404s a genuinely nonexistent id with no help from that filter) — see
        // crossTenantOrderIsBlockedByTheRealGuardFilter() below for the assertion that does.
        mvc.perform(get("/api/orders/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void accountOrdersIsReachableAndTenantScoped() throws Exception {
        mvc.perform(get("/api/account/orders").param("email", "nobody@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void recentOrderHistoryIsReachableAndTenantScoped() throws Exception {
        mvc.perform(get("/api/order-history/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void crossTenantOrderIsBlockedByTheRealGuardFilter() throws Exception {
        Tenant otherTenant = new Tenant();
        otherTenant.setSlug("order-exposure-cross-tenant-test");
        otherTenant.setDisplayName("Order Exposure Cross-Tenant Test");
        otherTenant.setActive(true);
        tenantRepository.saveAndFlush(otherTenant);

        Order order = new Order();
        order.setOrderTrackingNumber("ORDER-EXPOSURE-CROSS-TENANT-TEST");
        order.setTotalQuantity(1);
        order.setTotalPrice(new BigDecimal("19.99"));
        order.setStatus("Received");
        order.setTenantId(otherTenant.getId());

        Address shipping = new Address();
        shipping.setStreet("1 Cross Tenant Ln");
        shipping.setCity("Testville");
        shipping.setState("California");
        shipping.setCountry("United States");
        shipping.setZipCode("90210");
        order.setShippingAddress(shipping);

        Address billing = new Address();
        billing.setStreet("1 Cross Tenant Ln");
        billing.setCity("Testville");
        billing.setState("California");
        billing.setCountry("United States");
        billing.setZipCode("90210");
        order.setBillingAddress(billing);

        OrderItem item = new OrderItem();
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("19.99"));
        item.setProductId(1L);
        item.setImageUrl("https://example.test/order-exposure-cross-tenant.png");
        order.add(item);

        Order saved = orderRepository.saveAndFlush(order);
        entityManager.clear();

        // Requested with NO tenant header at all — resolves to the default "demo" tenant — reading an
        // order id that genuinely belongs to a different tenant. Unlike theOrderItemResourceStaysReachable
        // above, Spring Data REST's own findById would happily return this row (it really exists); only
        // TenantResourceGuardFilter's real, wired-in enforcement turns it into a 404. This is the
        // assertion that could not have been trusted under the old webAppContextSetup(context) harness.
        mvc.perform(get("/api/orders/" + saved.getId())).andExpect(status().isNotFound());

        // Sanity check the guard isn't simply blocking everything: the SAME id, requested with that
        // tenant's own header, is reachable.
        mvc.perform(get("/api/orders/" + saved.getId())
                        .header("X-Tenant-Id", "order-exposure-cross-tenant-test"))
                .andExpect(status().isOk());
    }
}
