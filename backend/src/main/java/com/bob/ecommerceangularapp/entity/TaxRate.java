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
 * A sales-tax rate for a region. Matched against an order's shipping address by {@code country} +
 * {@code state} (a blank {@code state} is a country-wide fallback), most-specific-wins. Stored as a
 * percentage (e.g. 7.25 = 7.25%). Country/state are matched on the same display names the checkout
 * sends (e.g. "United States" / "California").
 */
@Entity
@Table(name = "tax_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "country")
    private String country;

    /** Blank/null = applies to the whole country (fallback when no state-specific rate matches). */
    @Column(name = "state")
    private String state;

    @Column(name = "rate_percent")
    private BigDecimal ratePercent;

    @Column(name = "active")
    private boolean active;
}
