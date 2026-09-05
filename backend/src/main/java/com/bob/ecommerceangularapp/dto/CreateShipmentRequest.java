package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin body to fulfill an order from a warehouse. Carrier/tracking may be added later via status update. */
public record CreateShipmentRequest(
        @NotNull Long warehouseId,
        @Size(max = 64) String carrier,
        @Size(max = 128) String trackingNumber,
        @Size(max = 500) String note) {
}
