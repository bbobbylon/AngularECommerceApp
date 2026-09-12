package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.WebhookSubscription;

import java.util.Date;

/** List-view of a webhook subscription — the signing secret is masked, never shown in full again. */
public record WebhookSubscriptionView(
        Long id,
        String url,
        String maskedSecret,
        String eventTypes,
        boolean active,
        Date dateCreated) {

    public static WebhookSubscriptionView from(WebhookSubscription subscription) {
        return new WebhookSubscriptionView(subscription.getId(), subscription.getUrl(),
                mask(subscription.getSecret()), subscription.getEventTypes(), subscription.isActive(),
                subscription.getDateCreated());
    }

    private static String mask(String secret) {
        if (secret == null || secret.length() <= 6) {
            return "••••••••";
        }
        return secret.substring(0, 6) + "••••••••";
    }
}
