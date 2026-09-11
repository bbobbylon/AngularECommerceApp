package com.bob.ecommerceangularapp.dto;

/**
 * One SKU's stock at one warehouse, joined with the roadmap-#15 inventory naming so the admin sees
 * product/variant names, not bare SKUs. {@code variantLabel} is null for single-SKU products.
 */
public record WarehouseStockRow(
        String sku,
        String productName,
        String variantLabel,
        int quantity) {
}
