package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * One row per {@link Tenant} that has ever been put on a {@link BillingPlan} (roadmap #22, tenant
 * billing) — created lazily by {@code BillingService.assignPlan} on first assignment, not seeded
 * up front for every tenant. {@code tenantId} is a plain mapped column (real FK only in the DB), the
 * same convention {@code SavedPaymentMethod.tenantId} etc. already use.
 *
 * <p>Only Stripe references + display metadata are stored for the card on file — never raw card data
 * (PCI), identical shape to {@link SavedPaymentMethod}. Unlike a customer's list of many saved cards,
 * a billing account has exactly one card slot: {@code recordPaymentMethod} replaces it.
 *
 * <p>{@code currentPeriodEnd}/{@code status} are this app's own bookkeeping — recomputed by
 * {@code BillingService}, never read back from Stripe (no Stripe Subscription object is created; see
 * {@code BillingService} javadoc for why).
 */
@Entity
@Table(name = "tenant_billing_account", uniqueConstraints = @UniqueConstraint(name = "uk_billing_account_tenant", columnNames = "tenant_id"))
@Getter
@Setter
public class TenantBillingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "plan_id")
    private Long planId;

    /** NO_PLAN / ACTIVE / PAST_DUE / CANCELED. */
    @Column(name = "status", nullable = false)
    private String status = "NO_PLAN";

    @Column(name = "stripe_payment_method_id")
    private String stripePaymentMethodId;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_exp_month")
    private Integer cardExpMonth;

    @Column(name = "card_exp_year")
    private Integer cardExpYear;

    /** When the next charge is due. Reset by {@code assignPlan}, advanced a month on each successful charge. */
    @Column(name = "current_period_end")
    private Date currentPeriodEnd;

    @Column(name = "last_billed_at")
    private Date lastBilledAt;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
