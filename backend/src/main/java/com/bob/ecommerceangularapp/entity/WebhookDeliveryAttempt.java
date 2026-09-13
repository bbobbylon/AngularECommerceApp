package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Append-only delivery ledger (roadmap #23, Milestone D) — one row per HTTP attempt against a
 * {@link WebhookEvent}, never updated afterward. Shaped like {@code BillingInvoice}: every attempt,
 * success or failure, gets a row, so an admin can see the full retry history for an event.
 */
@Entity
@Table(name = "webhook_delivery_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "webhook_event_id", nullable = false)
    private Long webhookEventId;

    @Column(name = "webhook_subscription_id", nullable = false)
    private Long webhookSubscriptionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    /** SUCCEEDED | FAILED */
    @Column(name = "outcome", nullable = false)
    private String outcome;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "attempted_at")
    private Date attemptedAt;
}
