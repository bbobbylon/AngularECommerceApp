package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Closes the gap an explicit {@code tenant_id} predicate can't reach on its own (roadmap #21,
 * Milestone A — see the {@code tenant_id} javadoc on {@link com.bob.ecommerceangularapp.entity.Product}):
 * Spring Data REST's single-item resources (backed by {@code findById}/{@code getReferenceById}) go
 * straight to the repository with no query-building step a service could add a predicate to, and Spring
 * Data REST exposes no read-side event hook to intercept there either. This filter runs ahead of Spring
 * Data REST's own dispatch and 404s (never 403 — an id belonging to another tenant should look exactly
 * like an id that doesn't exist) any single-item request whose id isn't owned by the current request's
 * tenant.
 *
 * <p>Only the three Milestone-A single-item resources need this: {@code /api/products/{id}},
 * {@code /api/product-category/{id}}, {@code /api/orders/{id}}. Collection endpoints, search, and every
 * other resource are untouched here — collections already go through a service/specification that adds
 * an explicit tenant predicate (e.g. {@code /api/catalog/search} — see {@code ProductQueryService}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
public class TenantResourceGuardFilter extends OncePerRequestFilter {

    private record Guard(Pattern pattern, Function<Long, Function<Long, Boolean>> exists) {
    }

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final OrderRepository orderRepository;
    private final Guard[] guards;

    public TenantResourceGuardFilter(ProductRepository productRepository,
                                     ProductCategoryRepository productCategoryRepository,
                                     OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.orderRepository = orderRepository;
        this.guards = new Guard[] {
                new Guard(Pattern.compile("^/api/products/(\\d+)/?$"),
                        id -> tenantId -> productRepository.existsByIdAndTenantId(id, tenantId)),
                new Guard(Pattern.compile("^/api/product-category/(\\d+)/?$"),
                        id -> tenantId -> productCategoryRepository.existsByIdAndTenantId(id, tenantId)),
                new Guard(Pattern.compile("^/api/orders/(\\d+)/?$"),
                        id -> tenantId -> orderRepository.existsByIdAndTenantId(id, tenantId)),
        };
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        boolean singleItemMethod = "GET".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
        if (singleItemMethod) {
            String uri = request.getRequestURI();
            for (Guard guard : guards) {
                Matcher matcher = guard.pattern().matcher(uri);
                if (matcher.matches()) {
                    Long id = Long.valueOf(matcher.group(1));
                    Long tenantId = TenantContext.currentTenantId();
                    if (tenantId == null || !guard.exists().apply(id).apply(tenantId)) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Not found.\"}");
                        return;
                    }
                    break;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
