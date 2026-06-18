package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** The fully-resolved checkout totals breakdown the storefront renders (and the server records). */
public record QuoteResponse(
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal taxRatePercent,
        BigDecimal total,
        String shippingMethodCode) {
}
