package com.bob.ecommerceangularapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata for the Luv2Shop API. springdoc generates the spec from the controllers
 * automatically; this just supplies the human-facing title/description/contact and registers the
 * Bearer-JWT scheme so the Swagger UI "Authorize" button can attach a token for the protected
 * endpoints (order history, {@code /api/admin/**}) when Okta is configured.
 *
 * <p>Swagger UI: {@code /swagger-ui.html} · OpenAPI JSON: {@code /v3/api-docs}. Both can be disabled
 * in production via {@code springdoc.swagger-ui.enabled}/{@code springdoc.api-docs.enabled}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI luv2ShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Luv2Shop API")
                        .version("1.0.0")
                        .description("""
                                REST API for the Luv2Shop storefront: catalog, faceted search, cart/checkout,
                                Stripe payments, reviews, coupons, wishlist, newsletter, and the admin back-office.

                                Most endpoints are public. **Order history (`GET /api/orders/**`) and the admin
                                back-office (`/api/admin/**`) require a Bearer JWT once Okta is configured** —
                                click **Authorize** and paste an access token to call them. With no issuer
                                configured the API runs fully open for local development.""")
                        .contact(new Contact().name("Luv2Shop").email("support@luv2shop.example"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("OIDC access token (e.g. from Okta). Required only for order history and admin endpoints.")));
    }
}
