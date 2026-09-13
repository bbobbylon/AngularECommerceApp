package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * One cookie/storage consent decision, captured when a visitor accepts, rejects, or edits their
 * choices (roadmap #24). This is an <strong>append-only ledger</strong> — the same idiom as
 * {@link AuditLogEntry} and {@link BillingInvoice} — never updated in place: GDPR Art. 7(1) requires
 * demonstrating what was consented to <em>at the time</em>, which a single mutable current-state row
 * cannot do because it destroys the previous decision.
 *
 * <p>Deliberately carries no IP address or user-agent. Those are the conventional proof-of-consent
 * fields, but harvesting more personal data to prove a privacy choice is self-defeating.
 * {@code visitorId} is an opaque id the browser generates for itself and keeps in the one storage
 * key that is exempt from consent (the consent record itself is strictly necessary); {@code email}
 * is populated only when the visitor is already known to us, i.e. when we hold it regardless.
 *
 * <p>{@code necessary} is always true — the cart and the consent choice itself cannot be switched
 * off without breaking the store — and is stored rather than assumed so an old record still says
 * exactly what the categories were when it was written.
 */
@Entity
@Table(name = "consent_record", indexes = {
        @Index(name = "idx_consent_record_visitor", columnList = "visitor_id"),
        @Index(name = "idx_consent_record_email", columnList = "email"),
        @Index(name = "idx_consent_record_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {

    /** Consent captured from the storefront banner or its "Manage preferences" panel. */
    public static final String SOURCE_BANNER = "BANNER";

    /** Consent changed from the signed-in account settings page. */
    public static final String SOURCE_ACCOUNT = "ACCOUNT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy) — see {@link Product#getTenantId()}. */
    @Column(name = "tenant_id")
    private Long tenantId;

    /** Opaque, browser-generated id. Not derived from anything identifying. */
    @Column(name = "visitor_id", nullable = false, length = 64)
    private String visitorId;

    /** Only set when the visitor is already known to us; null for anonymous browsing. */
    @Column(name = "email")
    private String email;

    @Column(name = "necessary", nullable = false)
    private boolean necessary;

    @Column(name = "functional", nullable = false)
    private boolean functional;

    @Column(name = "analytics", nullable = false)
    private boolean analytics;

    @Column(name = "marketing", nullable = false)
    private boolean marketing;

    /**
     * Which version of the cookie/privacy policy this decision was made against. Bumping
     * {@code app.privacy.policy-version} re-prompts everyone, because consent to an older policy
     * is not consent to a new one.
     */
    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;

    /** {@link #SOURCE_BANNER} or {@link #SOURCE_ACCOUNT}. */
    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
