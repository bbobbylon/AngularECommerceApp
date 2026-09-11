package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.PlatformBillingPlanRequest;
import com.bob.ecommerceangularapp.entity.BillingPlan;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.BillingPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Platform-level billing-plan catalog management (roadmap #22) — {@code SuperAdmin}-only, same gate
 * as {@link PlatformTenantController} (already covered by the existing {@code /api/platform/**}
 * matcher in {@code SecurityConfig}, no new matcher needed).
 */
@RestController
@RequestMapping("/api/platform/billing-plans")
public class PlatformBillingPlanController {

    private final BillingPlanService billingPlanService;
    private final AuditLogService auditLogService;

    public PlatformBillingPlanController(BillingPlanService billingPlanService, AuditLogService auditLogService) {
        this.billingPlanService = billingPlanService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<BillingPlan> list() {
        return billingPlanService.list();
    }

    @PostMapping
    public ResponseEntity<BillingPlan> save(Authentication authentication, @Valid @RequestBody PlatformBillingPlanRequest request) {
        boolean isCreate = request.id() == null;
        BillingPlan saved = billingPlanService.save(request);
        auditLogService.record(authentication, isCreate ? "PLATFORM_BILLING_PLAN_CREATE" : "PLATFORM_BILLING_PLAN_UPDATE",
                "BillingPlan", String.valueOf(saved.getId()), saved.getName());
        return ResponseEntity.status(isCreate ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(Authentication authentication, @PathVariable Long id) {
        billingPlanService.deactivate(id);
        auditLogService.record(authentication, "PLATFORM_BILLING_PLAN_DEACTIVATE", "BillingPlan", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
