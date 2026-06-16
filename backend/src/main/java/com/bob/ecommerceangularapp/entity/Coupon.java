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

/** A discount code. Either percentOff or amountOff is set (percent takes precedence if both). */
@Entity
@Table(name = "coupon")
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
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
