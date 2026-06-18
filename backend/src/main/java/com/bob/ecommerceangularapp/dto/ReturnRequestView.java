package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;
import java.util.Date;

/** A return request as the storefront/admin sees it. */
public record ReturnRequestView(
        Long id,
        Long orderId,
        String orderTrackingNumber,
        String customerEmail,
        String reason,
        String status,
        BigDecimal refundAmount,
        String adminNote,
        boolean refunded,
        Date dateCreated) {
}
