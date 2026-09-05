package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/**
 * Top-line KPIs for the admin analytics dashboard. {@code growthPercent} is null when there was no
 * revenue last month to compare against (avoids a divide-by-zero rather than reporting a fake 0%).
 */
public record AnalyticsSummary(BigDecimal averageOrderValue, BigDecimal revenueThisMonth,
                                BigDecimal revenueLastMonth, Double growthPercent) {
}
