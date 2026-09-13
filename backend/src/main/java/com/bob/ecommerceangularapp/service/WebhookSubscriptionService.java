package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.WebhookDeliveryAttemptRepository;
import com.bob.ecommerceangularapp.dao.WebhookSubscriptionRepository;
import com.bob.ecommerceangularapp.dto.AdminWebhookSubscriptionRequest;
import com.bob.ecommerceangularapp.dto.WebhookDeliveryAttemptView;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

/**
 * Create/list/update/deactivate tenant webhook subscriptions (roadmap #23, Milestone B). Mirrors
 * {@code TaxShippingService}'s upsert-by-optional-id idiom: a null {@code id} creates a new
 * subscription (generating a fresh signing secret), a non-null one updates the existing row without
 * touching its secret. {@link #deactivate} is a soft flip of {@code active} — never a hard delete —
 * so a subscription's delivery history (Milestone D) always has a row to reference.
 */
@Service
public class WebhookSubscriptionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SECRET_PREFIX = "whsec_";

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository;

    public WebhookSubscriptionService(WebhookSubscriptionRepository webhookSubscriptionRepository,
                                      WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository) {
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.webhookDeliveryAttemptRepository = webhookDeliveryAttemptRepository;
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> listAll() {
        return webhookSubscriptionRepository.findAllByTenantId(TenantContext.currentTenantId());
    }

    @Transactional
    public WebhookSubscription save(AdminWebhookSubscriptionRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        WebhookSubscription subscription = request.id() != null
                ? webhookSubscriptionRepository.findByIdAndTenantId(request.id(), tenantId)
                        .orElseThrow(() -> new IllegalArgumentException("Webhook subscription not found: " + request.id()))
                : WebhookSubscription.builder().tenantId(tenantId).secret(generateSecret()).build();
        subscription.setUrl(request.url().trim());
        subscription.setEventTypes(String.join(",", request.eventTypes()));
        subscription.setActive(request.active());
        return webhookSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void deactivate(Long id) {
        WebhookSubscription subscription = webhookSubscriptionRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Webhook subscription not found: " + id));
        subscription.setActive(false);
        webhookSubscriptionRepository.save(subscription);
    }

    /** Ownership-checked (Milestone C follow-up precedent) so one tenant can never page through another's deliveries. */
    @Transactional(readOnly = true)
    public Page<WebhookDeliveryAttemptView> listDeliveries(Long subscriptionId, Pageable pageable) {
        Long tenantId = TenantContext.currentTenantId();
        webhookSubscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook subscription not found: " + subscriptionId));
        return webhookDeliveryAttemptRepository
                .findAllByWebhookSubscriptionIdAndTenantIdOrderByAttemptedAtDesc(subscriptionId, tenantId, pageable)
                .map(WebhookDeliveryAttemptView::from);
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return SECRET_PREFIX + HexFormat.of().formatHex(bytes);
    }
}
