package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Date;
import java.util.List;

/**
 * Admin payload to issue an API key (roadmap #23, Milestone A). {@code authorities} reuses the
 * existing Admin/OrderManager/Viewer role strings verbatim — no new authority vocabulary.
 * {@code expiresAt} is optional; a null value never expires.
 */
public record AdminApiKeyRequest(
        @NotBlank(message = "Name is required") String name,
        @NotEmpty(message = "At least one authority is required") List<String> authorities,
        Date expiresAt) {
}
