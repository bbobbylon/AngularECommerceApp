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
 * A tenant's registration to receive webhook deliveries at {@code url} for a chosen set of
 * {@code eventTypes} (roadmap #23, Milestone B). {@code secret} is stored raw — unlike
 * {@code ApiKey.keyHash}, this app must re-read it on every delivery to compute the HMAC-SHA256
 * {@code X-Webhook-Signature} (Milestone D), so hashing it would make delivery impossible; this
 * mirrors how a provider like Stripe stores/re-displays its own webhook signing secrets.
 */
@Entity
@Table(name = "webhook_subscription")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "secret", nullable = false)
    private String secret;

    /** Comma-separated event type strings, e.g. "order.created,return.approved". */
    @Column(name = "event_types", nullable = false, length = 500)
    private String eventTypes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
