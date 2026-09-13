package com.bob.ecommerceangularapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Short-interval sweep for webhook delivery (roadmap #23, Milestone D) — mirrors
 * {@link MonthlyBillingScheduler}'s shape (a thin cron trigger only; all the actual logic lives in
 * {@link WebhookDeliveryService#deliverDue()}, which never throws). Cadence configurable via
 * {@code app.webhook.delivery-cron} (default: every 30 seconds), far shorter than billing's daily
 * sweep since a subscriber expects near-real-time notification, not next-day settlement.
 */
@Component
public class WebhookDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryScheduler.class);

    private final WebhookDeliveryService webhookDeliveryService;

    public WebhookDeliveryScheduler(WebhookDeliveryService webhookDeliveryService) {
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @Scheduled(cron = "${app.webhook.delivery-cron:*/30 * * * * *}")
    public void deliverDue() {
        int count = webhookDeliveryService.deliverDue();
        if (count > 0) {
            log.info("Webhook delivery sweep processed {} due event(s).", count);
        }
    }
}
