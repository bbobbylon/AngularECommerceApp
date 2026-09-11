package com.bob.ecommerceangularapp.dto;

/**
 * How well one active warehouse can cover an order's lines, so the admin UI can suggest where to
 * ship from. Sorted best-first (full coverage, then covered-line count, then warehouse priority).
 */
public record FulfillmentOption(
        Long warehouseId,
        String code,
        String name,
        int totalLines,
        int coveredLines,
        boolean fullCoverage) {
}
