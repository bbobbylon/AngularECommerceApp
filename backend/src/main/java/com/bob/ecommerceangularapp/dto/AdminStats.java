package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** Snapshot metrics for the admin dashboard. */
public record AdminStats(
        long totalProducts,
        long activeProducts,
        long lowStockProducts,
        long productsOnSale,
        long totalOrders,
        BigDecimal totalRevenue,
        long totalCustomers,
        long newsletterSubscribers) {
}
