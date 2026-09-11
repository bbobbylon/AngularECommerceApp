package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.Tenant;

import java.util.Date;

/**
 * Platform tenants-table row (roadmap #22) — {@code Tenant} itself no longer carries plan data (it
 * moved to {@code TenantBillingAccount}/{@code BillingPlan} in V20), so this joins the two in
 * {@code PlatformTenantService} rather than exposing the raw entity like Milestone B originally did.
 */
public record PlatformTenantView(
        Long id,
        String slug,
        String displayName,
        String contactEmail,
        boolean active,
        Date dateCreated,
        Long planId,
        String planName,
        String billingStatus) {

    public static PlatformTenantView withoutBilling(Tenant tenant) {
        return new PlatformTenantView(tenant.getId(), tenant.getSlug(), tenant.getDisplayName(),
                tenant.getContactEmail(), tenant.isActive(), tenant.getDateCreated(), null, null, "NO_PLAN");
    }
}
