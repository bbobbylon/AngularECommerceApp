package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Create/update payload for an admin-managed product variant. {@code id} is null for new variants and
 * set when editing an existing one (the admin form sends the full variant list per product, so the
 * service upserts by id and removes any omitted). {@code unitPrice} is an optional override.
 */
public record AdminVariantRequest(
        Long id,
        @NotBlank(message = "Variant SKU is required") String sku,
        String color,
        String size,
        BigDecimal unitPrice,
        @PositiveOrZero(message = "Stock must be ≥ 0") int unitsInStock,
        String imageUrl,
        int sortOrder,
        boolean active) {
}
