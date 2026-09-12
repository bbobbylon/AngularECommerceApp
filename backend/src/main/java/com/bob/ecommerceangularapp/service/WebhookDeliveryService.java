package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.WebhookDeliveryAttemptRepository;
import com.bob.ecommerceangularapp.dao.WebhookEventRepository;
import com.bob.ecommerceangularapp.dao.WebhookSubscriptionRepository;
import com.bob.ecommerceangularapp.entity.WebhookDeliveryAttempt;
import com.bob.ecommerceangularapp.entity.WebhookEvent;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * Delivers outbox rows to their subscription's URL, retrying with backoff (roadmap #23, Milestone D).
 * Mirrors {@link BillingService}'s {@code chargeDueAccounts()/chargeOne()/recordInvoice()} triad
 * exactly: every branch of a delivery attempt (success, non-2xx, network error, deactivated
 * subscription) writes a {@link WebhookDeliveryAttempt} row and returns normally — one event's
 * failure never blocks another's. Each outbound request is signed with HMAC-SHA256 over the raw
 * JSON payload, using the subscription's own secret, and sent as {@code X-Webhook-Signature} — the
 * receiver verifies it the same way Stripe's own webhook signature is documented to be verified.
 */
@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

    static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    static final String EVENT_TYPE_HEADER = "X-Webhook-Event";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** Attempt N's delay once it has failed, in minutes; the event is ABANDONED once attempts are exhausted. */
    private static final int[] BACKOFF_MINUTES = {1, 5, 15, 60, 360};
    private static final List<String> DUE_STATUSES = List.of("PENDING", "FAILED");

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final WebhookDeliveryAttemptRepository attemptRepository;
    private final RestClient webhookRestClient;

    public WebhookDeliveryService(WebhookEventRepository webhookEventRepository,
                                  WebhookSubscriptionRepository webhookSubscriptionRepository,
                                  WebhookDeliveryAttemptRepository attemptRepository,
                                  RestClient webhookRestClient) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.attemptRepository = attemptRepository;
        this.webhookRestClient = webhookRestClient;
    }

    /** Delivers every event whose {@code nextAttemptAt} is due, independently. Never throws. */
    @Transactional
    public int deliverDue() {
        List<WebhookEvent> due = webhookEventRepository.findAllByStatusInAndNextAttemptAtLessThanEqual(DUE_STATUSES, new Date());
        for (WebhookEvent event : due) {
            deliverOne(event);
        }
        if (!due.isEmpty()) {
            log.info("Webhook delivery sweep processed {} due event(s).", due.size());
        }
        return due.size();
    }

    void deliverOne(WebhookEvent event) {
        WebhookSubscription subscription = webhookSubscriptionRepository.findById(event.getWebhookSubscriptionId()).orElse(null);
        if (subscription == null || !subscription.isActive()) {
            event.setStatus("ABANDONED");
            event.setNextAttemptAt(null);
            webhookEventRepository.save(event);
            return;
        }

        int attemptNumber = event.getAttemptCount() + 1;
        try {
            String signature = sign(subscription.getSecret(), event.getPayload());
            var response = webhookRestClient.post()
                    .uri(subscription.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SIGNATURE_HEADER, signature)
                    .header(EVENT_TYPE_HEADER, event.getEventType())
                    .body(event.getPayload())
                    .retrieve()
                    .toBodilessEntity();
            recordAttempt(event, subscription, attemptNumber, "SUCCEEDED", response.getStatusCode().value(), null);
            event.setStatus("DELIVERED");
            event.setAttemptCount(attemptNumber);
            event.setLastAttemptedAt(new Date());
            event.setNextAttemptAt(null);
            webhookEventRepository.save(event);
        } catch (RestClientResponseException e) {
            recordAttempt(event, subscription, attemptNumber, "FAILED", e.getStatusCode().value(), e.getMessage());
            advanceAfterFailure(event, attemptNumber);
        } catch (Exception e) {
            recordAttempt(event, subscription, attemptNumber, "FAILED", null, e.getMessage());
            advanceAfterFailure(event, attemptNumber);
        }
    }

    private void advanceAfterFailure(WebhookEvent event, int attemptNumber) {
        event.setAttemptCount(attemptNumber);
        event.setLastAttemptedAt(new Date());
        if (attemptNumber >= BACKOFF_MINUTES.length) {
            event.setStatus("ABANDONED");
            event.setNextAttemptAt(null);
        } else {
            event.setStatus("FAILED");
            event.setNextAttemptAt(Date.from(Instant.now().plusSeconds(BACKOFF_MINUTES[attemptNumber - 1] * 60L)));
        }
        webhookEventRepository.save(event);
    }

    private void recordAttempt(WebhookEvent event, WebhookSubscription subscription, int attemptNumber,
                                String outcome, Integer httpStatusCode, String errorMessage) {
        WebhookDeliveryAttempt attempt = WebhookDeliveryAttempt.builder()
                .tenantId(event.getTenantId())
                .webhookEventId(event.getId())
                .webhookSubscriptionId(subscription.getId())
                .eventType(event.getEventType())
                .attemptNumber(attemptNumber)
                .outcome(outcome)
                .httpStatusCode(httpStatusCode)
                .errorMessage(errorMessage == null ? null : errorMessage.substring(0, Math.min(500, errorMessage.length())))
                .attemptedAt(new Date())
                .build();
        attemptRepository.save(attempt);
    }

    private static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Could not sign webhook payload", e);
        }
    }
}
