package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.BillingInvoice;

import java.math.BigDecimal;
import java.util.Date;

/** One row of a tenant's billing history (roadmap #22) — powers the admin Billing page's invoice list. */
public record BillingInvoiceView(
        Long id,
        String planName,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        Date attemptedAt) {

    public static BillingInvoiceView from(BillingInvoice invoice) {
        return new BillingInvoiceView(invoice.getId(), invoice.getPlanNameSnapshot(), invoice.getAmount(),
                invoice.getCurrency(), invoice.getStatus(), invoice.getFailureReason(), invoice.getAttemptedAt());
    }
}
