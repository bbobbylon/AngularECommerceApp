package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Admin payload to create/update a webhook subscription (roadmap #23, Milestone B). A null
 * {@code id} creates a new subscription (a fresh signing secret is generated); a non-null one
 * updates {@code url}/{@code eventTypes}/{@code active} on the existing row — the secret never
 * changes on an update, matching the DTO convention used by {@code TaxRateRequest}/
 * {@code ShippingMethodRequest} elsewhere in this codebase.
 */
public record AdminWebhookSubscriptionRequest(
        Long id,
        @NotBlank(message = "URL is required") String url,
        @NotEmpty(message = "At least one event type is required") List<String> eventTypes,
        boolean active) {
}
