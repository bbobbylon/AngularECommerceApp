package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.entity.ApiKey;
import com.bob.ecommerceangularapp.service.ApiKeyLookupService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (no Spring context) for deriving Spring Security authentication from an
 * {@code X-Api-Key} header (roadmap #23, Milestone A). Constructed directly with a mocked
 * {@link ApiKeyLookupService}, same style as {@link TenantResolutionFilterTest}.
 */
class ApiKeyAuthenticationFilterTest {

    private final ApiKeyLookupService apiKeyLookupService = mock(ApiKeyLookupService.class);
    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(apiKeyLookupService, "X-Api-Key");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private ApiKey apiKey(String prefix, String authorities) {
        ApiKey key = new ApiKey();
        key.setKeyPrefix(prefix);
        key.setAuthorities(authorities);
        return key;
    }

    @Test
    void validKeySetsAnAuthenticatedPrincipalWithItsAuthorities() throws ServletException, IOException {
        ApiKey key = apiKey("lsk_abcd1234", "OrderManager,Viewer");
        when(apiKeyLookupService.lookup("raw-secret")).thenReturn(Optional.of(key));
        when(apiKeyLookupService.isUsable(key)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/orders");
        request.addHeader("X-Api-Key", "raw-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo("apikey:lsk_abcd1234");
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("OrderManager", "Viewer");
    }

    @Test
    void missingHeaderLeavesContextUntouched() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unusableKeyLeavesContextUntouchedAndStillProceedsDownTheChain() throws ServletException, IOException {
        ApiKey key = apiKey("lsk_abcd1234", "Admin");
        when(apiKeyLookupService.lookup("stale-secret")).thenReturn(Optional.of(key));
        when(apiKeyLookupService.isUsable(key)).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/orders");
        request.addHeader("X-Api-Key", "stale-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200); // chain still invoked, no short-circuit here
    }
}
