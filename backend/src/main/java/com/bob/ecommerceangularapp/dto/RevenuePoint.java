package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** One day's worth of revenue for the admin analytics revenue-over-time chart. */
public record RevenuePoint(String date, BigDecimal revenue, long orderCount) {
}
