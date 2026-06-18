package com.bob.ecommerceangularapp.dto;

/**
 * A customer's referral standing: their shareable {@code code}, how many friends have completed a
 * first order ({@code completedReferrals}), and the total points earned from referrals.
 */
public record ReferralSummary(
        String email,
        String code,
        int completedReferrals,
        int pointsEarned,
        int referrerReward,
        int refereeReward) {
}
