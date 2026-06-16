package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;
import java.util.Date;

/** Flattened order row for the admin orders table (includes customer info Order hides from JSON). */
public record AdminOrderView(
        Long id,
        String orderTrackingNumber,
        String status,
        int totalQuantity,
        BigDecimal totalPrice,
        Date dateCreated,
        String customerName,
        String customerEmail) {
}
