package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Admin create/update payload for a tax rate ({@code id} null when creating). */
public record TaxRateRequest(
        Long id,
        @NotBlank(message = "Country is required") String country,
        String state,
        @NotNull(message = "Rate is required") @PositiveOrZero(message = "Rate must be ≥ 0") BigDecimal ratePercent,
        boolean active) {
}
