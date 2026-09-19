package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.Order;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Flattened order row for the customer-facing order-history page (mirrors {@code AdminOrderView}
 * minus the customer name/email — the caller already knows whose history they asked for). Backs
 * {@code AccountController.orders()} and {@code OrderHistoryController.recent()}, which replaced the
 * raw, unscoped Spring Data REST {@code Order} search/collection resources (roadmap #21 gap).
 */
public record OrderHistoryView(
        Long id,
        String orderTrackingNumber,
        int totalQuantity,
        BigDecimal totalPrice,
        String status,
        Date dateCreated) {

    public static OrderHistoryView of(Order order) {
        return new OrderHistoryView(order.getId(), order.getOrderTrackingNumber(), order.getTotalQuantity(),
                order.getTotalPrice(), order.getStatus(), order.getDateCreated());
    }
}
