package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Tenant-facing read-only billing summary (roadmap #22) — powers the admin Billing page.
 * {@code BillingService.getAccount} always returns one of these, even for a tenant with no billing
 * account row yet (status {@code NO_PLAN}, every other field null), rather than a 404 — the tenant
 * hasn't done anything wrong, it just hasn't been put on a plan yet.
 */
public record BillingAccountView(
        String status,
        String planName,
        BigDecimal monthlyPrice,
        String currency,
        String features,
        Date currentPeriodEnd,
        Date lastBilledAt,
        String cardBrand,
        String cardLast4,
        Integer cardExpMonth,
        Integer cardExpYear) {

    public static BillingAccountView noPlan() {
        return new BillingAccountView("NO_PLAN", null, null, null, null, null, null, null, null, null, null);
    }
}
