package com.bob.ecommerceangularapp.dto;

import java.util.Date;

/** Flattened shipment for both the admin orders panel and the customer's order-history lookup. */
public record ShipmentView(
        Long id,
        Long orderId,
        String orderTrackingNumber,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String carrier,
        String trackingNumber,
        String status,
        Date shippedAt,
        Date deliveredAt,
        String note,
        Date dateCreated) {
}
