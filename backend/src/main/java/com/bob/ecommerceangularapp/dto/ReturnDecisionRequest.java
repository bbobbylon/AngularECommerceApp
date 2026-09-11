package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Admin decision on a return. {@code action} is {@code APPROVE} or {@code DENY}; on approval a
 * {@code refundAmount} (optional — defaults to the order total) is refunded via Stripe when the order
 * was paid by card and Stripe is configured, otherwise the return is marked approved for a manual refund.
 */
public record ReturnDecisionRequest(
        @NotBlank(message = "Action is required") String action,
        BigDecimal refundAmount,
        String adminNote) {
}
