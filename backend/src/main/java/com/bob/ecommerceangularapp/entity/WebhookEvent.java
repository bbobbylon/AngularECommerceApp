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
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * One outbox row per matching active {@link WebhookSubscription} at the moment a domain event was
 * published (roadmap #23, Milestone C). {@code payload} is a JSON snapshot taken at enqueue time —
 * later changes to the underlying entity never affect what gets delivered. {@link WebhookDeliveryService}
 * (Milestone D) advances {@code status} PENDING -&gt; DELIVERED/FAILED -&gt; (eventually) ABANDONED,
 * mirroring {@code TenantBillingAccount}'s own mutable-row-plus-append-only-ledger shape
 * ({@link WebhookDeliveryAttempt} is the ledger).
 */
@Entity
@Table(name = "webhook_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "webhook_subscription_id", nullable = false)
    private Long webhookSubscriptionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "longtext")
    private String payload;

    /** PENDING | DELIVERED | FAILED | ABANDONED */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Date nextAttemptAt;

    @Column(name = "last_attempted_at")
    private Date lastAttemptedAt;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
