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

import java.math.BigDecimal;

/**
 * A selectable shipping option at checkout. {@code baseRate} is the flat charge; when
 * {@code freeOverThreshold} is set and the merchandise subtotal reaches it, shipping is free.
 * Identified by a stable {@code code} (the order records this).
 */
@Entity
@Table(name = "shipping_method")
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
