package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.entity.ApiKey;
import com.bob.ecommerceangularapp.service.ApiKeyLookupService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Derives a Spring Security {@code Authentication} from the {@code X-Api-Key} header — the same key
 * {@link TenantResolutionFilter} already resolves tenant identity from (roadmap #23, Milestone A).
 * Registered directly inside {@code SecurityConfig}'s {@code HttpSecurity} via
 * {@code addFilterBefore(..., BearerTokenAuthenticationFilter.class)} on both filter chains —
 * deliberately NOT a {@code @Component}/{@code @Order} bean, since that would register it a second
 * time as a generic servlet filter running entirely outside {@code FilterChainProxy}, before
 * {@code SecurityContextHolderFilter} has installed the request's security context; anything set
 * there would be silently discarded the moment the chain actually starts (see
 * {@link TenantResolutionFilter}'s javadoc for the full mechanism). Running inside the chain instead
 * — strictly after {@code SecurityContextHolderFilter} — mirrors exactly how
 * {@code BasicAuthenticationFilter}/{@code BearerTokenAuthenticationFilter}/X.509 client-cert auth set
 * authentication: none of them persist via a {@code SecurityContextRepository} either, because
 * nothing later in the same request needs a reload.
 *
 * <p>A missing/unknown/revoked/expired key is a no-op here (the context stays anonymous) rather than
 * an error — {@link TenantResolutionFilter} already rejects an invalid key with a 404 earlier in the
 * same request, so by the time this filter runs a present key is always valid; a request with no key
 * at all just proceeds to whatever the chain's normal (Okta JWT or anonymous) authentication produces.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyLookupService apiKeyLookupService;
    private final String apiKeyHeaderName;

    public ApiKeyAuthenticationFilter(ApiKeyLookupService apiKeyLookupService, String apiKeyHeaderName) {
        this.apiKeyLookupService = apiKeyLookupService;
        this.apiKeyHeaderName = apiKeyHeaderName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getHeader(apiKeyHeaderName);
        if (StringUtils.hasText(rawKey)) {
            Optional<ApiKey> apiKey = apiKeyLookupService.lookup(rawKey.trim()).filter(apiKeyLookupService::isUsable);
            apiKey.ifPresent(key -> SecurityContextHolder.getContext().setAuthentication(toAuthentication(key)));
        }
        filterChain.doFilter(request, response);
    }

    /** The 3-arg constructor marks the token authenticated by default — same idiom X.509 auth uses. */
    private PreAuthenticatedAuthenticationToken toAuthentication(ApiKey apiKey) {
        List<GrantedAuthority> authorities = Arrays.stream(apiKey.getAuthorities().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        return new PreAuthenticatedAuthenticationToken("apikey:" + apiKey.getKeyPrefix(), null, authorities);
    }
}
