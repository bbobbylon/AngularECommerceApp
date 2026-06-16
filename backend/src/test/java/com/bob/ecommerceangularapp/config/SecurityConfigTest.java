package com.bob.ecommerceangularapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the JWT groups-claim → Spring Security authorities mapping that gates
 * {@code /api/admin/**}. Exercises {@link SecurityConfig#adminAwareConverter()} directly.
 *
 * <p>Spring Security 7 attaches a framework {@code FactorGrantedAuthority} (e.g. {@code FACTOR_BEARER})
 * to every JWT authentication; we filter those out so the assertions speak only to the group → role
 * mapping under test.
 */
class SecurityConfigTest {

    private JwtAuthenticationConverter converterWithClaim(String claim) {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "adminClaim", claim);
        return config.adminAwareConverter();
    }

    /** Group-derived authorities only — excludes Spring Security's own FACTOR_* authorities. */
    private List<String> groupAuthorities(AbstractAuthenticationToken auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("FACTOR_"))
                .toList();
    }

    private Jwt jwtWith(String claimName, Object value) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("user")
                .claim(claimName, value)
                .build();
    }

    @Test
    void mapsGroupsClaimToAuthorities() {
        JwtAuthenticationConverter converter = converterWithClaim("groups");
        AbstractAuthenticationToken auth = converter.convert(jwtWith("groups", List.of("Admin", "Users")));
        assertThat(groupAuthorities(auth)).containsExactlyInAnyOrder("Admin", "Users");
    }

    @Test
    void honoursACustomClaimName() {
        JwtAuthenticationConverter converter = converterWithClaim("roles");
        AbstractAuthenticationToken auth = converter.convert(jwtWith("roles", List.of("Admin")));
        assertThat(groupAuthorities(auth)).containsExactly("Admin");
    }

    @Test
    void noGroupAuthoritiesWhenClaimAbsent() {
        JwtAuthenticationConverter converter = converterWithClaim("groups");
        AbstractAuthenticationToken auth = converter.convert(jwtWith("other", "x"));
        assertThat(groupAuthorities(auth)).isEmpty();
    }

    @Test
    void noGroupAuthoritiesWhenClaimIsNotACollection() {
        JwtAuthenticationConverter converter = converterWithClaim("groups");
        AbstractAuthenticationToken auth = converter.convert(jwtWith("groups", "Admin"));
        assertThat(groupAuthorities(auth)).isEmpty();
    }
}
