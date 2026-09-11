package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Admin create payload for a coupon. Set percentOff or amountOff. */
public record CouponRequest(
        @NotBlank(message = "Code is required") String code,
        String description,
        Integer percentOff,
        BigDecimal amountOff,
        BigDecimal minSpend,
        boolean active,
        LocalDate expiresAt) {
}
