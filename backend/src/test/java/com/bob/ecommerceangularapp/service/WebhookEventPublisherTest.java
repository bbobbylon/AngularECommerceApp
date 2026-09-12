package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.WebhookEventRepository;
import com.bob.ecommerceangularapp.dao.WebhookSubscriptionRepository;
import com.bob.ecommerceangularapp.entity.WebhookEvent;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring/DB) for the transactional-outbox writer (roadmap #23, Milestone C).
 * Needs {@link TenantContext} scaffolding — {@code publish} reads the tenant id ambiently.
 */
class WebhookEventPublisherTest {

    private static final Long TENANT_ID = 7L;

    private final WebhookSubscriptionRepository subscriptionRepository = mock(WebhookSubscriptionRepository.class);
    private final WebhookEventRepository eventRepository = mock(WebhookEventRepository.class);
    private final WebhookEventPublisher publisher =
            new WebhookEventPublisher(subscriptionRepository, eventRepository, new ObjectMapper());

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private static WebhookSubscription subscription(long id, String eventTypes) {
        return WebhookSubscription.builder().id(id).tenantId(TENANT_ID).eventTypes(eventTypes).active(true).build();
    }

    @Test
    void publish_writesOneRowPerMatchingActiveSubscription() {
        when(subscriptionRepository.findAllByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of(
                subscription(1L, "order.created,return.approved"),
                subscription(2L, "order.created")));

        publisher.publish("order.created", Map.of("orderId", 42));

        verify(eventRepository, times(2)).save(any(WebhookEvent.class));
    }

    @Test
    void publish_skipsSubscriptionsNotOptedIntoThisEventType() {
        when(subscriptionRepository.findAllByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of(
                subscription(1L, "return.approved,return.denied")));

        publisher.publish("order.created", Map.of("orderId", 42));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_noMatchingSubscriptionsIsANoOp() {
        when(subscriptionRepository.findAllByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of());

        publisher.publish("order.created", Map.of("orderId", 42));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_writtenEventCarriesTenantSubscriptionAndSerializedPayload() {
        when(subscriptionRepository.findAllByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(List.of(
                subscription(5L, "order.created")));

        publisher.publish("order.created", Map.of("orderId", 42));

        var captor = org.mockito.ArgumentCaptor.forClass(WebhookEvent.class);
        verify(eventRepository).save(captor.capture());
        WebhookEvent saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getWebhookSubscriptionId()).isEqualTo(5L);
        assertThat(saved.getEventType()).isEqualTo("order.created");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getNextAttemptAt()).isNotNull();
        assertThat(saved.getPayload()).contains("\"orderId\":42");
    }
}
