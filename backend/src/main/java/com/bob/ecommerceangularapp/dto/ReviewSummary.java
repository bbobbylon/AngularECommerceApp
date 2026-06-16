package com.bob.ecommerceangularapp.dto;

/** Aggregate rating info for a product. {@code distribution[i]} is the count of (i+1)-star reviews. */
public record ReviewSummary(double average, long count, int[] distribution) {
}
