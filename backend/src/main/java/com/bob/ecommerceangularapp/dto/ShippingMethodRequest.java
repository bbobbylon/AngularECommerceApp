package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Admin create/update payload for a shipping method ({@code id} null when creating). */
public record ShippingMethodRequest(
        Long id,
        @NotBlank(message = "Code is required") String code,
        @NotBlank(message = "Name is required") String name,
        @NotNull(message = "Base rate is required") @PositiveOrZero(message = "Base rate must be ≥ 0") BigDecimal baseRate,
        BigDecimal freeOverThreshold,
        String estimatedDays,
        int sortOrder,
        boolean active) {
}
