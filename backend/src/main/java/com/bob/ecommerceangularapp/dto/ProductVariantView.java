package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/**
 * Read model for a single product variant as the storefront sees it. {@code unitPrice} is already
 * resolved (the variant's override, or the product's price) and {@code imageUrl} falls back to the
 * product image — so the frontend can render a variant without re-deriving anything.
 */
public record ProductVariantView(
        Long id,
        String sku,
        String color,
        String size,
        String label,
        BigDecimal unitPrice,
        int unitsInStock,
        boolean inStock,
        String imageUrl) {
}
