package com.bob.ecommerceangularapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Milestone 3 — security.
 *
 * <p>The secured chain is only active once an Okta (or any OIDC) issuer URI is configured via
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. When it is set, the app validates
 * incoming Bearer JWTs and requires authentication for {@code GET /api/orders/**} (order history).
 *
 * <p>When no issuer is configured the open chain applies, so the catalog/cart/checkout API stays
 * fully usable for local Milestone 1/2 development without standing up an identity provider.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "issuer-uri")
    SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").authenticated()
                        // Back-office: require authentication. For production, further restrict to an
                        // admin group/role (e.g. .hasAuthority('SCOPE_admin')) — see docs/SECURITY.md.
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
