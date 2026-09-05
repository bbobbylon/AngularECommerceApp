package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** A best-seller row for the admin analytics dashboard, aggregated from order line items. */
public record TopProduct(Long productId, String name, long unitsSold, BigDecimal revenue) {
}
