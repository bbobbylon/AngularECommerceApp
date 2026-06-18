package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A customer's rewards standing: redeemable {@code balance}, {@code lifetimePoints} (drives the tier),
 * the {@code tier} name + progress to the next, the cash {@code redeemableValue} of the balance, and
 * recent ledger history.
 */
public record LoyaltySummary(
        String email,
        int balance,
        int lifetimePoints,
        String tier,
        String nextTier,
        int pointsToNextTier,
        BigDecimal redeemableValue,
        List<LoyaltyTransactionView> history) {
}
