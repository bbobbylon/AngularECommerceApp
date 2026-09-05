package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Admin payload for a single manual stock adjustment (the inline edit on the Inventory page). */
public record InventoryAdjustmentRequest(
        @NotNull(message = "Quantity is required") @Min(value = 0, message = "Quantity can't be negative") Integer quantity,
        String note) {
}
