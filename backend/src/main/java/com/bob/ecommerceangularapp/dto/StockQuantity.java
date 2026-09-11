package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

/** One SKU-quantity pair in a warehouse stock update (quantities are clamped at zero server-side). */
public record StockQuantity(
        @NotBlank String sku,
        int quantity) {
}
