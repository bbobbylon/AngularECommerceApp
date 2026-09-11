package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A selectable shipping option at checkout. {@code baseRate} is the flat charge; when
 * {@code freeOverThreshold} is set and the merchandise subtotal reaches it, shipping is free.
 * Identified by a per-tenant-stable {@code code} (the order records this).
 */
@Entity
@Table(name = "shipping_method", uniqueConstraints = @UniqueConstraint(name = "uk_shipping_method_tenant_code", columnNames = {"tenant_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy, Milestone C). See {@link Product#getTenantId()} for the isolation rationale. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "base_rate")
    private BigDecimal baseRate;

    /** Null = never free; otherwise free when the merchandise subtotal reaches this amount. */
    @Column(name = "free_over_threshold")
    private BigDecimal freeOverThreshold;

    @Column(name = "estimated_days")
    private String estimatedDays;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "active")
    private boolean active;
}
