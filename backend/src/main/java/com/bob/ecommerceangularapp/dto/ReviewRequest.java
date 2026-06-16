package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Create payload for a product review. */
public record ReviewRequest(
        @NotNull(message = "Product is required") Long productId,
        @NotBlank(message = "Name is required") String authorName,
        @Min(value = 1, message = "Rating must be 1–5") @Max(value = 5, message = "Rating must be 1–5") int rating,
        String comment) {
}
