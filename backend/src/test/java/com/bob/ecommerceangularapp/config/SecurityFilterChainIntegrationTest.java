package com.bob.ecommerceangularapp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
