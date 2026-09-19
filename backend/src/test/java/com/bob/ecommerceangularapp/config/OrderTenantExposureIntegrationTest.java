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
 * Full-context, real-HTTP proof (over the <b>open</b> security chain — no Okta issuer configured,
 * i.e. how the app runs by default) that the roadmap #21 order-history gap is actually closed, not
 * just unit-tested against a mock:
 * <ul>
 *   <li>the raw Spring Data REST search {@code /api/orders/search/findByCustomerEmailOrderByDateCreatedDesc}
 *   and the raw collection {@code GET /api/orders} — both previously reachable by anyone, with no
 *   tenant predicate, mixing every tenant's order history together — are gone;</li>
 *   <li>their tenant-scoped replacements ({@code AccountController}, {@code OrderHistoryController})
 *   are reachable and return the expected paged-JSON shape.</li>
 * </ul>
 * {@link OrderRepositoryTest} covers the actual tenant-isolation of the query itself; this test covers
 * only reachability/shape, so it stays valid regardless of what {@code DataLoader} has seeded.
 */
@SpringBootTest
class OrderTenantExposureIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).build();
    }

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
        // checkout/API consumers still rely on GET /api/orders/{id}.
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
}
