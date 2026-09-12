package com.bob.ecommerceangularapp.config;

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
}
