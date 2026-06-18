package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Admin payload to issue a gift card. {@code code} is optional — a random one is generated when blank.
 * {@code initialBalance} seeds both the initial and remaining balance.
 */
public record AdminGiftCardRequest(
        String code,
        @NotNull(message = "Initial balance is required") @Positive(message = "Balance must be > 0") BigDecimal initialBalance,
        String recipientEmail,
        boolean active) {
}
