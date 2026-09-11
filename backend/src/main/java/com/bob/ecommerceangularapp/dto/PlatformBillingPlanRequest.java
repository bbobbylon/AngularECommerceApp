package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Platform-admin upsert body for a {@code BillingPlan} (roadmap #22) — null id creates, non-null
 * updates (same pattern as {@code PlatformTenantRequest}).
 */
public record PlatformBillingPlanRequest(
        Long id,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.00", message = "monthlyPrice can't be negative") BigDecimal monthlyPrice,
        @Size(max = 3) String currency,
        @Size(max = 2000) String features,
        Integer sortOrder,
        Boolean active) {
}
