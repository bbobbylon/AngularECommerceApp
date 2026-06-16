package com.bob.ecommerceangularapp.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight abuse protection for the public, unauthenticated write endpoints (reviews, coupon
 * validation, newsletter signup): a per-IP fixed-window rate limit plus a request-body size cap.
 * Backed by Caffeine (already on the classpath) so there's no extra dependency and no shared state to
 * manage — swap in Redis/Bucket4j if you need limits shared across instances.
 *
 * <p>Runs just after {@link RequestIdFilter} so rejections still carry a correlation id. Read paths
 * and the core checkout flow are deliberately not limited.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    /** Public, mutating, abuse-prone path prefixes to guard. */
    private static final List<String> LIMITED_PREFIXES = List.of(
            "/api/reviews", "/api/coupons", "/api/newsletter");

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long MAX_BODY_BYTES = 64 * 1024; // 64 KB — these payloads are tiny

    private final Cache<String, AtomicInteger> hits = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(50_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            reject(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request body too large.");
            return;
        }

        String key = clientIp(request) + "|" + request.getRequestURI();
        int count = hits.get(key, k -> new AtomicInteger()).incrementAndGet();
        if (count > MAX_REQUESTS_PER_MINUTE) {
            response.setHeader("Retry-After", "60");
            reject(response, 429, "Too many requests — please slow down and try again shortly.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimited(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return false;
        }
        String uri = request.getRequestURI();
        return LIMITED_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    /** Honours the first X-Forwarded-For hop (behind a proxy/LB), else the socket address. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
