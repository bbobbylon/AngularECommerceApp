package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Create/update payload for admin product management. */
public record AdminProductRequest(
        @NotBlank(message = "SKU is required") String sku,
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotNull(message = "Price is required") @PositiveOrZero(message = "Price must be ≥ 0") BigDecimal unitPrice,
        BigDecimal originalPrice,
        String imageUrl,
        boolean active,
        @PositiveOrZero(message = "Stock must be ≥ 0") int unitsInStock,
        @NotNull(message = "Category is required") Long categoryId) {
}
