package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;

/** The single best-value automatic promotion for a subtotal, as resolved by PromotionService. */
public record AppliedPromotion(String name, BigDecimal discount) {
}
