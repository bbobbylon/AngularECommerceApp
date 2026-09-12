package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.WebhookSubscription;

import java.util.Date;

/**
 * Returned exactly once, when a subscription is first created — the only place its full signing
 * secret is ever exposed. Every later read uses {@link WebhookSubscriptionView} (masked) instead.
 */
public record WebhookSubscriptionCreatedView(
        Long id,
        String url,
        String secret,
        String eventTypes,
        boolean active,
        Date dateCreated) {

    public static WebhookSubscriptionCreatedView from(WebhookSubscription subscription) {
        return new WebhookSubscriptionCreatedView(subscription.getId(), subscription.getUrl(),
                subscription.getSecret(), subscription.getEventTypes(), subscription.isActive(),
                subscription.getDateCreated());
    }
}
