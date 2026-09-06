package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An automatic, no-code discount applied at checkout when its window/min-spend conditions are met
 * (unlike {@link Coupon}, which requires the customer to enter a code). Either percentOff or
 * amountOff is set (percent takes precedence if both). A null startsAt/endsAt means unbounded on
 * that side.
 */
@Entity
@Table(name = "promotion")
@Getter
@Setter
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy, Milestone C). See {@link Product#getTenantId()} for the isolation rationale. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "percent_off")
    private Integer percentOff;

    @Column(name = "amount_off")
    private BigDecimal amountOff;

    @Column(name = "min_spend")
    private BigDecimal minSpend;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "starts_at")
    private LocalDate startsAt;

    @Column(name = "ends_at")
    private LocalDate endsAt;
}
