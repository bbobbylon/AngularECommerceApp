package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.WebhookEventRepository;
import com.bob.ecommerceangularapp.dao.WebhookSubscriptionRepository;
import com.bob.ecommerceangularapp.entity.WebhookEvent;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * The transactional outbox writer (roadmap #23, Milestone C). {@link #publish} is called from the
 * same mutation points {@code AuditLogService.record(...)} already instruments (order placed,
 * shipment shipped/delivered, return approved/denied) and reads {@link TenantContext} ambiently, the
 * same idiom {@code AuditLogService} uses — every call site here is request-scoped, unlike
 * {@code BillingService}'s explicit-tenant-parameter style, which exists to serve a tenant-less
 * background sweep. Deliberately {@code @Transactional} (not {@code REQUIRES_NEW}): it joins the
 * caller's existing transaction, so a rolled-back order never produces a stray webhook event. Writes
 * one {@link WebhookEvent} row per active subscription that opted into this event type; no outbound
 * HTTP happens here — delivery is {@code WebhookDeliveryService}'s job (Milestone D).
 */
@Service
public class WebhookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventPublisher.class);

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    public WebhookEventPublisher(WebhookSubscriptionRepository webhookSubscriptionRepository,
                                 WebhookEventRepository webhookEventRepository,
                                 ObjectMapper objectMapper) {
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publish(String eventType, Object payload) {
        Long tenantId = TenantContext.currentTenantId();
        List<WebhookSubscription> candidates = webhookSubscriptionRepository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .filter(sub -> subscribesTo(sub, eventType))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        String json = serialize(payload);
        Date now = new Date();
        for (WebhookSubscription subscription : candidates) {
            WebhookEvent event = WebhookEvent.builder()
                    .tenantId(tenantId)
                    .webhookSubscriptionId(subscription.getId())
                    .eventType(eventType)
                    .payload(json)
                    .status("PENDING")
                    .attemptCount(0)
                    .nextAttemptAt(now)
                    .build();
            webhookEventRepository.save(event);
        }
    }

    private static boolean subscribesTo(WebhookSubscription subscription, String eventType) {
        for (String type : subscription.getEventTypes().split(",")) {
            if (type.trim().equalsIgnoreCase(eventType)) {
                return true;
            }
        }
        return false;
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload: {}", e.getMessage());
            return "{}";
        }
    }
}
