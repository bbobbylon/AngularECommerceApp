package com.bob.ecommerceangularapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The one genuinely new outbound-HTTP surface this app has (roadmap #23, Milestone D) — every other
 * external call (Stripe, email) goes through a dedicated SDK/starter, not a raw HTTP client. Short
 * timeouts so one unreachable/slow subscriber URL can't stall the delivery scheduler.
 */
@Configuration
public class WebhookClientConfig {

    @Bean
    public RestClient webhookRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * This app's per-web-stack starter ({@code spring-boot-starter-webmvc}) doesn't transitively pull
     * a Jackson 2 {@code jackson-databind} jar the way the old {@code spring-boot-starter-web} did, so
     * Spring Boot's own Jackson auto-configuration never registers a {@link ObjectMapper} bean here —
     * confirmed via {@code dependency:tree}: the only {@code com.fasterxml.jackson.databind} jar on the
     * classpath comes transitively from springdoc-openapi, not from any Spring-owned starter. Rather
     * than depend on that incidental, easily-broken transitive path, {@link WebhookEventPublisher}
     * gets its own explicit bean, same as the dedicated {@link #webhookRestClient()} above.
     */
    @Bean
    public ObjectMapper webhookObjectMapper() {
        return new ObjectMapper();
    }
}
