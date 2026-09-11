package com.bob.ecommerceangularapp.dto;

import java.util.Date;

/** One row of the inventory audit log (admin "Recent adjustments" panel). */
public record InventoryAdjustmentView(
        Long id,
        String sku,
        String productName,
        int previousQuantity,
        int newQuantity,
        int delta,
        String source,
        String note,
        Date dateCreated) {
}
