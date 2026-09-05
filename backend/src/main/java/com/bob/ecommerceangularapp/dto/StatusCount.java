package com.bob.ecommerceangularapp.dto;

/** Order count for one status value, for the admin analytics order-status breakdown. */
public record StatusCount(String status, long count) {
}
