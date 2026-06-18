package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** "Notify me when back in stock" signup. {@code variantSku} is optional (product-level when null). */
public record StockNotificationRequest(
        @NotBlank(message = "Email is required") @Email(message = "A valid email is required") String email,
        @NotNull(message = "Product is required") Long productId,
        String variantSku) {
}
