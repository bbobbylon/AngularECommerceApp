package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** Result of validating a coupon: whether it applies, the discount amount, and a user message. */
public record CouponResponse(boolean valid, String code, String description, BigDecimal discount, String message) {

    public static CouponResponse invalid(String code, String message) {
        return new CouponResponse(false, code, null, BigDecimal.ZERO, message);
    }
}
