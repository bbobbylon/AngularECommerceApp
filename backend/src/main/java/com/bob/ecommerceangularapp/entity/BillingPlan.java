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

import java.math.BigDecimal;
import java.util.Date;

/**
 * A priced plan a tenant can be put on (roadmap #22, tenant billing). Platform-level, like
 * {@link Tenant} itself — no {@code tenant_id}, since a plan is offered to every tenant, not owned by
 * one. {@code monthlyPrice} is charged off-session on each {@link TenantBillingAccount}'s renewal by
 * {@code MonthlyBillingScheduler}; changing a plan's price only affects future charges, never past
 * {@link BillingInvoice} rows (those snapshot the price at charge time). {@code active} soft-retires a
 * plan without breaking tenants already on it (matches {@code TaxRate}/{@code ShippingMethod}/
 * {@code Tenant.active}).
 */
@Entity
@Table(name = "billing_plan", uniqueConstraints = @UniqueConstraint(name = "uk_billing_plan_name", columnNames = "name"))
@Getter
@Setter
public class BillingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "monthly_price", nullable = false)
    private BigDecimal monthlyPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /** Newline-delimited feature bullets — plain text, no new parsing dependency needed. */
    @Column(name = "features", length = 2000)
    private String features;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
