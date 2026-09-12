package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.WebhookDeliveryAttempt;

import java.util.Date;

/** One row of a subscription's delivery history (roadmap #23, Milestone D). */
public record WebhookDeliveryAttemptView(
        Long id,
        Long webhookEventId,
        String eventType,
        int attemptNumber,
        String outcome,
        Integer httpStatusCode,
        String errorMessage,
        Date attemptedAt) {

    public static WebhookDeliveryAttemptView from(WebhookDeliveryAttempt attempt) {
        return new WebhookDeliveryAttemptView(attempt.getId(), attempt.getWebhookEventId(),
                attempt.getEventType(), attempt.getAttemptNumber(), attempt.getOutcome(),
                attempt.getHttpStatusCode(), attempt.getErrorMessage(), attempt.getAttemptedAt());
    }
}
