package com.bob.ecommerceangularapp.config;

import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class MyDataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {

        HttpMethod[] unsupportedActions = {HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH};

        // make Product and ProductCategory read-only
        disableHttpMethods(config, Product.class, unsupportedActions);
        disableHttpMethods(config, ProductCategory.class, unsupportedActions);

        // expose entity ids in the JSON responses
        config.exposeIdsFor(Product.class, ProductCategory.class);

        // allow the Angular dev server to call the API
        cors.addMapping(config.getBasePath() + "/**")
                .allowedOrigins("http://localhost:4200");
    }

    private void disableHttpMethods(RepositoryRestConfiguration config, Class<?> domainType, HttpMethod[] actions) {
        config.getExposureConfiguration()
                .forDomainType(domainType)
                .withItemExposure((metadata, httpMethods) -> httpMethods.disable(actions))
                .withCollectionExposure((metadata, httpMethods) -> httpMethods.disable(actions));
    }
}
