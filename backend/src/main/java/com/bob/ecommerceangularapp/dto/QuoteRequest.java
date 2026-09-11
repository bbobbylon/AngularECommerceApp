package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/**
 * Inputs for a checkout totals quote: the merchandise subtotal, the destination region (country/state
 * names, matching how the checkout sends an address), an optional coupon, and the chosen shipping
 * method code. The server computes the authoritative shipping + tax + total from these.
 */
public record QuoteRequest(
        BigDecimal subtotal,
        String country,
        String state,
        String couponCode,
        String shippingMethodCode) {
}
