package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** A selectable shipping option as the checkout sees it. */
public record ShippingMethodView(
        Long id,
        String code,
        String name,
        BigDecimal baseRate,
        BigDecimal freeOverThreshold,
        String estimatedDays) {
}
