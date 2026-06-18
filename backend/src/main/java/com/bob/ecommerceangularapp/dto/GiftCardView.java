package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** Result of checking a gift card at checkout: whether it's usable and the balance available. */
public record GiftCardView(boolean valid, String code, BigDecimal balance, String message) {

    public static GiftCardView invalid(String code, String message) {
        return new GiftCardView(false, code, BigDecimal.ZERO, message);
    }
}
