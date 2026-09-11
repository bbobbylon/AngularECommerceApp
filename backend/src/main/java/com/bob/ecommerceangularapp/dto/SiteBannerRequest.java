package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin upsert payload for the single site banner. */
public record SiteBannerRequest(
        @NotBlank(message = "Message is required") String message,
        String linkUrl,
        String linkText,
        boolean active) {
}
