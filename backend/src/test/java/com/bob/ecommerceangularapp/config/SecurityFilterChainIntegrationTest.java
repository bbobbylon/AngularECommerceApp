package com.bob.ecommerceangularapp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration test for the <b>secured</b> {@code SecurityFilterChain} — the behaviour once an OIDC
 * issuer is configured. It activates that chain by setting a (fake) {@code issuer-uri} and supplying a
 * stub {@link JwtDecoder} so no IdP/network is needed; requests are authenticated with Spring
 * Security's {@code jwt()} post-processor (which sets the authentication directly, bypassing the
 * decoder). This proves the real authorization rules — complementing {@link SecurityConfigTest}, which
 * unit-tests only the claim→authority mapping:
 * <ul>
 *   <li>public endpoints stay open,</li>
 *   <li>{@code /api/orders/**} and {@code /api/admin/**} require authentication,</li>
 *   <li>{@code /api/admin/**} additionally requires the <b>admin</b> authority — not just any login.</li>
 * </ul>
 */
@SpringBootTest(properties =
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.test/oauth2/default")
class SecurityFilterChainIntegrationTest {

    @TestConfiguration
    static class StubJwtDecoderConfig {
        // Stops the resource server from fetching JWKS from the fake issuer at startup. Never actually
        // invoked — the jwt() post-processor authenticates requests without decoding a real token.
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("stub decoder — not used in tests");
            };
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicCatalogIsReachableWithoutAuth() throws Exception {
        mvc.perform(get("/api/catalog/search")).andExpect(status().isOk());
    }

    @Test
    void ordersRequireAuthentication() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminRejectsAnonymous() throws Exception {
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminRejectsAuthenticatedNonAdmin() throws Exception {
        mvc.perform(get("/api/admin/stats").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAllowsUserWithAdminAuthority() throws Exception {
        mvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin"))))
                .andExpect(status().isOk());
    }

    @Test
    void accountRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/account?email=test@example.com")).andExpect(status().isUnauthorized());
    }

    @Test
    void newsletterSendNowRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/newsletter/send-now")).andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthIsPublicForProbes() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void actuatorMetricsRequireAuthentication() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    // ----- RBAC (roadmap #19): OrderManager and Viewer are scoped admin-tier roles -----

    @Test
    void viewerCanReadTheAdminBackOffice() throws Exception {
        mvc.perform(get("/api/admin/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Viewer"))))
                .andExpect(status().isOk());
    }

    @Test
    void orderManagerCanReadTheAdminBackOffice() throws Exception {
        mvc.perform(get("/api/admin/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("OrderManager"))))
                .andExpect(status().isOk());
    }

    @Test
    void viewerCannotUpdateOrderStatus() throws Exception {
        mvc.perform(put("/api/admin/orders/1/status").param("status", "SHIPPED")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Viewer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void orderManagerCanReachTheOrderStatusEndpoint() throws Exception {
        // Order 1 doesn't exist in the empty test DB, so this 404s past the security layer — the point
        // is that it is NOT rejected as 401/403, proving OrderManager authorization actually passed.
        mvc.perform(put("/api/admin/orders/1/status").param("status", "SHIPPED")
                        .with(jwt().authorities(new SimpleGrantedAuthority("OrderManager"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void orderManagerCannotCreateACategory() throws Exception {
        mvc.perform(post("/api/admin/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Gadgets\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("OrderManager"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotCreateACategory() throws Exception {
        mvc.perform(post("/api/admin/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Gadgets\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Viewer"))))
                .andExpect(status().isForbidden());
    }

    // ----- Fulfillment (roadmap #20): shipping is an order-management action -----

    @Test
    void orderManagerCanReachTheShipmentEndpoints() throws Exception {
        // Same pattern as the order-status case: a 4xx past the security layer (order 1 doesn't
        // exist) proves authorization passed — the point is it is NOT 401/403.
        mvc.perform(post("/api/admin/orders/1/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("OrderManager"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        mvc.perform(put("/api/admin/shipments/1/status").param("status", "SHIPPED")
                        .with(jwt().authorities(new SimpleGrantedAuthority("OrderManager"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void viewerCannotCreateShipments() throws Exception {
        mvc.perform(post("/api/admin/orders/1/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Viewer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void orderManagerCannotConfigureWarehouses() throws Exception {
        mvc.perform(post("/api/admin/warehouses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ATL\",\"name\":\"East\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("OrderManager"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateACategory() throws Exception {
        mvc.perform(post("/api/admin/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Gadgets\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin"))))
                .andExpect(status().isCreated());
    }

    // ----- Multi-tenancy platform tier (roadmap #21, Milestone B): SuperAdmin only, separate from RBAC -----

    @Test
    void platformRejectsAnonymous() throws Exception {
        mvc.perform(get("/api/platform/tenants")).andExpect(status().isUnauthorized());
    }

    @Test
    void platformRejectsRegularAdmin() throws Exception {
        mvc.perform(get("/api/platform/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAllowsSuperAdmin() throws Exception {
        mvc.perform(get("/api/platform/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SuperAdmin"))))
                .andExpect(status().isOk());
    }
}
