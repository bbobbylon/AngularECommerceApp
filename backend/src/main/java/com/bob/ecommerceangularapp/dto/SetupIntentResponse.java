package com.bob.ecommerceangularapp.dto;

/**
 * Response for starting an "add a card" flow. {@code enabled} is false when Stripe isn't configured
 * (the UI then shows a graceful "payments not set up" note); otherwise {@code clientSecret} drives
 * Stripe Elements' confirmCardSetup.
 */
public record SetupIntentResponse(boolean enabled, String clientSecret) {
}
