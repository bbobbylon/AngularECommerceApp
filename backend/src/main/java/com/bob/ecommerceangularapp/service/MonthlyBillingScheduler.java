package com.bob.ecommerceangularapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily sweep for tenant billing (roadmap #22) — a daily cadence, not a fixed calendar day, since
 * each tenant's own {@code currentPeriodEnd} determines when it's actually due. Every branch inside
 * {@link BillingService#chargeDueAccounts()} writes an invoice and returns normally, so this stays
 * enabled everywhere (no Stripe key needed — accounts are simply skipped with a recorded reason).
 * Cadence configurable via {@code app.billing.cron} (default: daily at 4am).
 */
@Component
public class MonthlyBillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyBillingScheduler.class);

    private final BillingService billingService;

    public MonthlyBillingScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    @Scheduled(cron = "${app.billing.cron:0 0 4 * * *}")
    public void chargeDueAccounts() {
        int count = billingService.chargeDueAccounts();
        if (count > 0) {
            log.info("Billing sweep processed {} due account(s).", count);
        }
    }
}
