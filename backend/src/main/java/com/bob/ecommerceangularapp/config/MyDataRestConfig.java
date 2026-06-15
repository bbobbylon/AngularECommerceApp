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
 * Locks the Spring Data REST endpoints down to read-only and wires up CORS for the
 * Angular dev server. POST/PUT/PATCH/DELETE are disabled on both domain types so the
 * auto-generated REST API only exposes safe read operations for the catalog.
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

        // expose entity ids in the JSON responses (off by default in Spring Data REST)
        config.exposeIdsFor(Product.class, ProductCategory.class, Country.class, State.class, Order.class);

        // allow the Angular dev server to call the API
        cors.addMapping(config.getBasePath() + "/**")
                .allowedOrigins("http://localhost:4200");
    }

    private void disableHttpMethods(Class<?> domainType, RepositoryRestConfiguration config,
                                    HttpMethod[] unsupportedActions) {
        config.getExposureConfiguration()
                .forDomainType(domainType)
                .withItemExposure((metadata, httpMethods) -> httpMethods.disable(unsupportedActions))
                .withCollectionExposure((metadata, httpMethods) -> httpMethods.disable(unsupportedActions));
    }
}
