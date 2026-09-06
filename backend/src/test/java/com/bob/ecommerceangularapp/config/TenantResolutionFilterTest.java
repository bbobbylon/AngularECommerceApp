package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.entity.Tenant;
import com.bob.ecommerceangularapp.service.TenantResolutionService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (no Spring context) for tenant resolution precedence (roadmap #21, Milestone A):
 * header/query-param &gt; subdomain &gt; configured default, plus the unknown-tenant 404.
 */
class TenantResolutionFilterTest {

    private final TenantResolutionService tenantResolutionService = mock(TenantResolutionService.class);

    @AfterEach
    void clearContext() {
        // Guard against a failing assertion leaking tenant state into another test in this JVM.
        TenantContext.clear();
    }

    private TenantResolutionFilter filter(String baseDomain) {
        return new TenantResolutionFilter(tenantResolutionService, "X-Tenant-Id", baseDomain, "demo");
    }

    private Tenant tenant(String slug, long id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setSlug(slug);
        t.setActive(true);
        return t;
    }

    @Test
    void headerWinsOverEverythingElse() throws ServletException, IOException {
        when(tenantResolutionService.resolveBySlug("acme")).thenReturn(Optional.of(tenant("acme", 2L)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("X-Tenant-Id", "acme");
        request.addParameter("tenant", "demo");
        request.addHeader("Host", "demo.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter("example.com").doFilter(request, response, chain);

        assertThat(chain.tenantIdSeen).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void queryParamWinsOverSubdomainAndDefault() throws ServletException, IOException {
        when(tenantResolutionService.resolveBySlug("acme")).thenReturn(Optional.of(tenant("acme", 2L)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addParameter("tenant", "acme");
        request.addHeader("Host", "demo.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter("example.com").doFilter(request, response, chain);

        assertThat(chain.tenantIdSeen).isEqualTo(2L);
    }

    @Test
    void subdomainWinsOverDefaultWhenBaseDomainConfigured() throws ServletException, IOException {
        when(tenantResolutionService.resolveBySlug("acme")).thenReturn(Optional.of(tenant("acme", 2L)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("Host", "acme.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter("example.com").doFilter(request, response, chain);

        assertThat(chain.tenantIdSeen).isEqualTo(2L);
    }

    @Test
    void fallsBackToDefaultSlugWithNoOtherSignal() throws ServletException, IOException {
        when(tenantResolutionService.resolveBySlug("demo")).thenReturn(Optional.of(tenant("demo", 1L)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("Host", "localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter("").doFilter(request, response, chain);

        assertThat(chain.tenantIdSeen).isEqualTo(1L);
    }

    @Test
    void unknownTenant404sAndNeverReachesTheChain() throws ServletException, IOException {
        when(tenantResolutionService.resolveBySlug("ghost")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("X-Tenant-Id", "ghost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter("").doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(chain.invoked).isFalse();
    }

    @Test
    void tenantContextIsClearedAfterTheRequest() throws ServletException, IOException {
        when(tenantResolutionService.resolveBySlug("demo")).thenReturn(Optional.of(tenant("demo", 1L)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter("").doFilter(request, response, new MockFilterChain());

        assertThat(TenantContext.currentTenantId()).isNull();
    }

    /** Records the tenant id visible to downstream code, mirroring how a real controller would see it. */
    private static final class RecordingFilterChain extends MockFilterChain {
        boolean invoked = false;
        Long tenantIdSeen;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            invoked = true;
            tenantIdSeen = TenantContext.currentTenantId();
        }
    }
}
