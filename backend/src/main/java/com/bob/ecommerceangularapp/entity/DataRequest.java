package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * A data-subject request — "send me my data" (GDPR Art. 15/20, portability) or "erase my data"
 * (Art. 17, right to be forgotten) — raised from the storefront (roadmap #24).
 *
 * <p><strong>The token is the identity check.</strong> Anyone can type an email address into a
 * form, so a request does nothing until it is confirmed from a link mailed to that address — the
 * same email-proves-identity idiom {@code Customer.unsubscribeToken} already established in M6.
 * Without it, an export endpoint would hand anyone's order history to whoever guessed their
 * address, and an erasure endpoint would be a vandalism tool. The token is single-use and expires
 * ({@code app.privacy.request-ttl-hours}, default 48).
 *
 * <p>Note what erasure does <em>not</em> do: it never deletes the {@link Customer} row.
 * {@code Customer.orders} is mapped {@code CascadeType.ALL}, so deleting a customer would take
 * their whole order history with it — records that tax and accounting law require keeping. Erasure
 * therefore anonymizes (see {@code DataRequestService#erase}), which is what Art. 17(3)(b) and (e)
 * contemplate anyway.
 */
@Entity
@Table(name = "data_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_data_request_token", columnNames = "token"),
        indexes = {
                @Index(name = "idx_data_request_email", columnList = "email"),
                @Index(name = "idx_data_request_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataRequest {

    /** Art. 15/20 — hand back everything we hold, in a portable format. */
    public static final String TYPE_EXPORT = "EXPORT";

    /** Art. 17 — erase what we can, anonymize what we must keep. */
    public static final String TYPE_ERASURE = "ERASURE";

    /** Raised, but nobody has proved they can read the mailbox yet. */
    public static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";

    /** Mailbox proved; for an export the bundle is now downloadable. */
    public static final String STATUS_VERIFIED = "VERIFIED";

    /** Export downloaded, or erasure carried out. Terminal. */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** Never confirmed before the token's TTL lapsed. Terminal. */
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy) — see {@link Product#getTenantId()}. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "email", nullable = false)
    private String email;

    /** {@link #TYPE_EXPORT} or {@link #TYPE_ERASURE}. */
    @Column(name = "request_type", nullable = false, length = 16)
    private String requestType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /** Opaque single-use secret mailed to the subject. Never shown in any list view. */
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    /**
     * What actually happened, in counts — e.g. "anonymized customer; deleted 3 wishlist items".
     * Deliberately free of the subject's own data: an erasure record that quotes the erased email
     * back has not erased much.
     */
    @Column(name = "result_summary", length = 1000)
    private String resultSummary;

    @Column(name = "expires_at")
    private Date expiresAt;

    @Column(name = "verified_at")
    private Date verifiedAt;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
