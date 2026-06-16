package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Validate/apply a coupon against a cart subtotal. */
public record CouponValidateRequest(
        @NotBlank(message = "Code is required") String code,
        @NotNull(message = "Subtotal is required") BigDecimal subtotal) {
}
