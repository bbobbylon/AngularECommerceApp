package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Append-only ledger, one row per {@code MonthlyBillingScheduler} billing <i>attempt</i> for a
 * {@link TenantBillingAccount} — success, failure, or skipped (roadmap #22, tenant billing). Never
 * updated or deleted, same idiom as {@link AuditLogEntry}. {@code planNameSnapshot}/{@code amount}
 * are captured at charge time rather than joined live from {@link BillingPlan}, so a later price
 * change never rewrites history and a plan change mid-cycle is never prorated against a stale price.
 */
@Entity
@Table(name = "billing_invoice",
        indexes = {
                @Index(name = "idx_billing_invoice_tenant", columnList = "tenant_id"),
                @Index(name = "idx_billing_invoice_account", columnList = "billing_account_id"),
        })
@Getter
@Setter
public class BillingInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "billing_account_id", nullable = false)
    private Long billingAccountId;

    @Column(name = "plan_name_snapshot")
    private String planNameSnapshot;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    /** SUCCEEDED / FAILED / SKIPPED. */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "attempted_at")
    @CreationTimestamp
    private Date attemptedAt;
}
