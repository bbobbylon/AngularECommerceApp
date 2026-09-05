package com.bob.ecommerceangularapp.dto;

/**
 * A single sellable inventory line — either a product with no variants (sku = the product's own SKU,
 * variantLabel null) or one variant of a product sold per-variant (sku = the variant's SKU,
 * variantLabel = its color/size label). Powers the admin Inventory page.
 */
public record InventoryItemView(
        String sku,
        Long productId,
        String productName,
        String variantLabel,
        int unitsInStock,
        boolean lowStock,
        boolean active) {
}
