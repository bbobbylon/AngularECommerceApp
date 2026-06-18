package com.bob.ecommerceangularapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically sends recovery emails for idle carts. The send is gated inside
 * {@link AbandonedCartService#remindStale()} (no-ops without SMTP), so this stays enabled everywhere.
 * Cadence configurable via {@code app.abandoned-cart.cron} (default: every 15 minutes).
 */
@Component
public class AbandonedCartScheduler {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartScheduler.class);

    private final AbandonedCartService abandonedCartService;

    public AbandonedCartScheduler(AbandonedCartService abandonedCartService) {
        this.abandonedCartService = abandonedCartService;
    }

    @Scheduled(cron = "${app.abandoned-cart.cron:0 */15 * * * *}")
    public void sendRecoveryEmails() {
        int sent = abandonedCartService.remindStale();
        if (sent > 0) {
            log.info("Abandoned-cart sweep sent {} reminder(s).", sent);
        }
    }
}
