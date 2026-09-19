package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.entity.Country;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.entity.State;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Locks the Spring Data REST endpoints down to read-only: POST/PUT/PATCH/DELETE are disabled on the
 * exposed domain types so the auto-generated REST API only offers safe read operations for the
 * catalog. (CORS for the whole API is centralized in {@link SecurityConfig}.)
 */
@Configuration
public class MyDataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {

        HttpMethod[] unsupportedActions = {
                HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH
        };

        disableHttpMethods(Product.class, config, unsupportedActions);
        disableHttpMethods(ProductCategory.class, config, unsupportedActions);
        disableHttpMethods(Country.class, config, unsupportedActions);
        disableHttpMethods(State.class, config, unsupportedActions);
        disableHttpMethods(Order.class, config, unsupportedActions);

        // Order's plain collection listing (GET /api/orders?sort=...) has no tenant predicate — Spring
        // Data REST's default findAll-backed collection resource can't take one — so it returned every
        // tenant's orders mixed together (full names/addresses/items/totals) to anyone who could reach
        // /api/orders/** (unauthenticated when Okta isn't configured). Nothing legitimate reads it: the
        // item resource (GET /api/orders/{id}, tenant-guarded by TenantResourceGuardFilter) stays on,
        // and customer-facing order history now goes through the tenant-scoped AccountController /
        // OrderHistoryController instead (roadmap #21 gap closed).
        config.getExposureConfiguration()
                .forDomainType(Order.class)
                .withCollectionExposure((metadata, httpMethods) -> httpMethods.disable(HttpMethod.GET));

        // expose entity ids in the JSON responses (off by default in Spring Data REST)
        config.exposeIdsFor(Product.class, ProductCategory.class, Country.class, State.class, Order.class);

        // NOTE: CORS is centralized in SecurityConfig#corsConfigurationSource (a servlet-level
        // CorsFilter that governs SDR + custom controllers uniformly and is driven by the
        // app.cors.allowed-origins property). Configuring it here too would create a second,
        // localhost-only policy that rejects the deployed frontend's origin. See docs/DEPLOYMENT.md.
        // allow the Angular dev server to call the API
        cors.addMapping(config.getBasePath() + "/**")
                .allowedOrigins("http://localhost:4200", "http://localhost:4251");
    }

    private void disableHttpMethods(Class<?> domainType, RepositoryRestConfiguration config,
                                    HttpMethod[] unsupportedActions) {
        config.getExposureConfiguration()
                .forDomainType(domainType)
                .withItemExposure((metadata, httpMethods) -> httpMethods.disable(unsupportedActions))
                .withCollectionExposure((metadata, httpMethods) -> httpMethods.disable(unsupportedActions));
    }
}
