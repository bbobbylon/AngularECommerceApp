package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Platform-admin upsert body for a {@code Tenant} (roadmap #21, Milestone B) — null id creates,
 * non-null updates (same pattern as {@code WarehouseRequest}). {@code slug} is restricted to the
 * subdomain-safe character set {@code TenantResolutionFilter} expects.
 */
public record PlatformTenantRequest(
        Long id,
        @NotBlank @Size(max = 63) @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug may only contain lowercase letters, digits, and hyphens.")
        String slug,
        @NotBlank @Size(max = 255) String displayName,
        @Email @Size(max = 255) String contactEmail,
        String plan,
        Boolean active) {
}
