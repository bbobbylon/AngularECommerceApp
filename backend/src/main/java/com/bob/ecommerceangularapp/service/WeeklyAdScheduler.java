package com.bob.ecommerceangularapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires the weekly marketing email on a cron schedule (default: Mondays 09:00 server time,
 * configurable via {@code app.newsletter.cron}). The actual send is gated inside
 * {@link NewsletterService#sendWeeklyBlast()} — it no-ops cleanly when email isn't configured,
 * so this can stay enabled in every environment.
 */
@Component
public class WeeklyAdScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyAdScheduler.class);

    private final NewsletterService newsletterService;

    public WeeklyAdScheduler(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @Scheduled(cron = "${app.newsletter.cron:0 0 9 * * MON}")
    public void sendWeeklyNewsletter() {
        log.info("Running scheduled weekly newsletter blast...");
        int sent = newsletterService.sendWeeklyBlast();
        log.info("Scheduled weekly newsletter blast complete ({} recipient(s)).", sent);
    }
}
