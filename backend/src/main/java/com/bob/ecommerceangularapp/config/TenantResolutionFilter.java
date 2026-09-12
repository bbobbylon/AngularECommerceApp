package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.entity.ApiKey;
import com.bob.ecommerceangularapp.entity.Tenant;
import com.bob.ecommerceangularapp.service.ApiKeyLookupService;
import com.bob.ecommerceangularapp.service.TenantResolutionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves which tenant a request belongs to (roadmap #21, Milestone A) and binds it to
 * {@link TenantContext} for the duration of the request — the same set/clear-in-finally idiom as
 * {@link RequestIdFilter}, placed right after it so rate limiting ({@link RateLimitFilter}) and
 * everything downstream (including Spring Security's JWT auth, and Hibernate's OSIV session-open,
 * which — for a servlet+MVC app — happens inside {@code DispatcherServlet}, strictly after every
 * servlet filter) can rely on it.
 *
 * <p>Resolution order: (1) the {@code X-Tenant-Id} header or {@code ?tenant=} query param (the
 * local-dev/tooling escape hatch — {@code localhost} has no real subdomain), carrying the tenant's
 * {@code slug}; (2) a subdomain parsed from {@code Host} against {@code app.tenant.base-domain} (unset
 * by default, so inert locally); (3) {@code app.tenant.default-slug} — keeps the app working with zero
 * tenant config, matching the existing Okta/Stripe/Mail graceful-degradation precedent. An unknown or
 * inactive tenant 404s immediately.
 *
 * <p>{@code /api/platform/**} (roadmap #21, Milestone B) is skipped like actuator/swagger below — it's
 * platform-level, not scoped to any single tenant, and skipping it here (rather than resolving normally)
 * avoids a real lockout: a superadmin's browser could still carry a stale {@code X-Tenant-Id} for a
 * tenant they just deactivated through that very API, which would otherwise 404 here before the request
 * ever reaches the {@code SuperAdmin}-gated route.
 *
 * <p>Roadmap #23, Milestone A adds a fourth, <i>higher</i>-precedence resolution step ahead of all of
 * the above: an {@code X-Api-Key} header resolves tenant identity directly via
 * {@link ApiKeyLookupService}, bypassing slug resolution entirely for the request — this lets a
 * headless caller identify itself with only its key, no {@code X-Tenant-Id} needed. An unknown/
 * revoked/expired key gets the exact same generic 404 as an unknown slug; this filter never touches
 * {@code SecurityContextHolder} — deriving Spring Security authorities from the same key is
 * {@code ApiKeyAuthenticationFilter}'s job, registered separately inside {@code SecurityConfig}'s
 * {@code HttpSecurity} (see its javadoc for why: this filter runs before {@code FilterChainProxy}
 * even starts, so anything it sets on the security context would be silently discarded).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TenantResolutionFilter extends OncePerRequestFilter {

    /** Paths with no meaningful "current tenant" — skipped entirely. */
    private static final String[] SKIPPED_PREFIXES = {"/actuator", "/swagger-ui", "/v3/api-docs", "/api/platform"};

    private static final String MDC_KEY = "tenant";

    private final TenantResolutionService tenantResolutionService;
    private final ApiKeyLookupService apiKeyLookupService;
    private final String headerName;
    private final String apiKeyHeaderName;
    private final String baseDomain;
    private final String defaultSlug;

    public TenantResolutionFilter(TenantResolutionService tenantResolutionService,
                                  ApiKeyLookupService apiKeyLookupService,
                                  @Value("${app.tenant.header:X-Tenant-Id}") String headerName,
                                  @Value("${app.api-key.header:X-Api-Key}") String apiKeyHeaderName,
                                  @Value("${app.tenant.base-domain:}") String baseDomain,
                                  @Value("${app.tenant.default-slug:demo}") String defaultSlug) {
        this.tenantResolutionService = tenantResolutionService;
        this.apiKeyLookupService = apiKeyLookupService;
        this.headerName = headerName;
        this.apiKeyHeaderName = apiKeyHeaderName;
        this.baseDomain = baseDomain;
        this.defaultSlug = defaultSlug;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        for (String prefix : SKIPPED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String apiKeyHeader = request.getHeader(apiKeyHeaderName);
        if (StringUtils.hasText(apiKeyHeader)) {
            resolveByApiKey(apiKeyHeader.trim(), request, response, filterChain);
            return;
        }

        String slug = resolveSlug(request);
        Optional<Tenant> tenant = tenantResolutionService.resolveBySlug(slug);
        if (tenant.isEmpty()) {
            writeUnknownStore(response);
            return;
        }

        Long tenantId = tenant.get().getId();
        TenantContext.set(tenantId);
        MDC.put(MDC_KEY, slug);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            TenantContext.clear();
        }
    }

    private void resolveByApiKey(String rawKey, HttpServletRequest request, HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {
        Optional<ApiKey> apiKey = apiKeyLookupService.lookup(rawKey).filter(apiKeyLookupService::isUsable);
        if (apiKey.isEmpty()) {
            writeUnknownStore(response);
            return;
        }

        TenantContext.set(apiKey.get().getTenantId());
        MDC.put(MDC_KEY, "apikey:" + apiKey.get().getKeyPrefix());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            TenantContext.clear();
        }
    }

    private void writeUnknownStore(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unknown store.\"}");
    }

    private String resolveSlug(HttpServletRequest request) {
        String header = request.getHeader(headerName);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        String queryParam = request.getParameter("tenant");
        if (StringUtils.hasText(queryParam)) {
            return queryParam.trim();
        }
        String subdomainSlug = subdomainFromHost(request.getHeader("Host"));
        if (subdomainSlug != null) {
            return subdomainSlug;
        }
        return defaultSlug;
    }

    /** {@code acme.example.com} against base domain {@code example.com} -&gt; {@code "acme"}; else null. */
    private String subdomainFromHost(String host) {
        if (!StringUtils.hasText(baseDomain) || !StringUtils.hasText(host)) {
            return null;
        }
        String hostname = host.split(":")[0].trim();
        String suffix = "." + baseDomain;
        if (hostname.endsWith(suffix) && hostname.length() > suffix.length()) {
            return hostname.substring(0, hostname.length() - suffix.length());
        }
        return null;
    }
}
