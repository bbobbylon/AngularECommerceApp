package com.bob.ecommerceangularapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Milestone 3 — security, with the production hardening pass on top.
 *
 * <p>The secured chain is only active once an Okta (or any OIDC) issuer URI is configured via
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. When it is set, the app validates
 * incoming Bearer JWTs, requires authentication for {@code GET /api/orders/**}, and requires the
 * <b>admin role</b> for {@code /api/admin/**} (derived from a configurable groups claim).
 *
 * <p>When no issuer is configured the open chain applies, so the catalog/cart/checkout API stays
 * fully usable for local development without standing up an identity provider (graceful degradation).
 *
 * <p>Both chains apply the same response-header hardening (CSP, HSTS, frame/referrer/permissions).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** JWT claim that carries the user's group/role memberships (Okta default: "groups"). */
    @Value("${app.security.admin-claim:groups}")
    private String adminClaim;

    /** Membership value required to reach the admin back-office. */
    @Value("${app.security.admin-role:Admin}")
    private String adminRole;

    /**
     * Browser origins allowed to call the API cross-origin. Comma-separated; defaults to the two
     * local dev/compose origins. In a cloud deploy set {@code APP_CORS_ALLOWED_ORIGINS} to the
     * deployed frontend's URL (e.g. the Cloud Run frontend URL) — see docs/DEPLOYMENT.md.
     */
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:4250}")
    private List<String> allowedOrigins;

    /** Strict CSP for the JSON API: no scripts at all, locked to same-origin. */
    private static final String STRICT_CSP =
            "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'";

    /**
     * Relaxed CSP applied ONLY to the Swagger UI / OpenAPI paths: Swagger UI ships inline scripts and
     * styles, so it needs {@code script-src 'self' 'unsafe-inline'}. Still same-origin-only — the strict
     * policy above keeps protecting every other (JSON) response.
     */
    private static final String SWAGGER_CSP =
            "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self' 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'";

    /** Matches the springdoc-served documentation endpoints. */
    private static final RequestMatcher SWAGGER_PATHS = request -> {
        String uri = request.getRequestURI();
        return uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    };

    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "issuer-uri")
    SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                        // Back-office requires the admin role (not just any authenticated user).
                        .requestMatchers("/api/admin/**").hasAuthority(adminRole)
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(adminAwareConverter())))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        applyHardening(http);
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        applyHardening(http);
        return http.build();
    }

    /**
     * Single source of truth for CORS. Spring Security's {@code .cors(withDefaults())} (on both the
     * open and secured chains) picks this bean up by name and applies it via a servlet-level
     * {@code CorsFilter} that runs <i>before</i> the dispatcher — so it governs <b>every</b> endpoint
     * uniformly: the Spring Data REST catalog resources <i>and</i> the custom {@code @RestController}s
     * (checkout, catalog search, admin, reviews, …). This replaces the previous scattered, hardcoded
     * {@code @CrossOrigin} annotations + the SDR CORS mapping, which would otherwise reject (403) a
     * request from the deployed frontend's origin at the MVC layer even after the filter allowed it.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        // Bearer tokens travel in the Authorization header (not cookies), so credentialed CORS isn't
        // needed; keeping it off lets the allowlist stay explicit without the wildcard restrictions.
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Maps the configured groups claim (e.g. Okta "groups") into Spring Security authorities.
     * Package-private so it can be unit-tested directly (see {@code SecurityConfigTest}).
     */
    JwtAuthenticationConverter adminAwareConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object claim = jwt.getClaim(adminClaim);
            if (claim instanceof Collection<?> groups) {
                return groups.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(g -> (GrantedAuthority) new SimpleGrantedAuthority(g.toString()))
                        .collect(Collectors.toList());
            }
            return List.of();
        });
        return converter;
    }

    /**
     * Response-header hardening shared by every filter chain. The CSP is applied per-path: the strict
     * policy on everything by default, and a Swagger-compatible one only on the docs endpoints (the two
     * matchers are mutually exclusive, so exactly one CSP header is written per response).
     */
    private void applyHardening(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "geolocation=(), microphone=(), camera=()"))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(SWAGGER_PATHS,
                        new ContentSecurityPolicyHeaderWriter(SWAGGER_CSP)))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(request -> !SWAGGER_PATHS.matches(request),
                        new ContentSecurityPolicyHeaderWriter(STRICT_CSP))));
    }
}
