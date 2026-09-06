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

import java.math.BigDecimal;
import java.time.LocalDate;

/** A discount code. Either percentOff or amountOff is set (percent takes precedence if both). */
@Entity
@Table(name = "coupon", uniqueConstraints = @UniqueConstraint(name = "uk_coupon_tenant_code", columnNames = {"tenant_id", "code"}))
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy, Milestone C). See {@link Product#getTenantId()} for the isolation rationale. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "percent_off")
    private Integer percentOff;

    @Column(name = "amount_off")
    private BigDecimal amountOff;

    @Column(name = "min_spend")
    private BigDecimal minSpend;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "expires_at")
    private LocalDate expiresAt;
}
