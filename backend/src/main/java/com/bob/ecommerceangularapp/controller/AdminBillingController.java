package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dto.BillingAccountView;
import com.bob.ecommerceangularapp.dto.BillingInvoiceView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.dto.RecordPaymentMethodRequest;
import com.bob.ecommerceangularapp.dto.SetupIntentResponse;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant-facing billing view (roadmap #22) — read-only plan/status/invoice history for any
 * back-office role (falls under the existing {@code /api/admin/**} GET matcher covering
 * {@code Admin}/{@code OrderManager}/{@code Viewer}), plus self-service card management restricted
 * to {@code Admin} by the existing {@code /api/admin/**} catch-all. The plan itself is
 * SuperAdmin-assigned only — see {@link PlatformTenantController#assignPlan}.
 */
@RestController
@RequestMapping("/api/admin/billing")
public class AdminBillingController {

    private final BillingService billingService;
    private final AuditLogService auditLogService;

    public AdminBillingController(BillingService billingService, AuditLogService auditLogService) {
        this.billingService = billingService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public BillingAccountView get() {
        return billingService.getAccount(TenantContext.currentTenantId());
    }

    @GetMapping("/invoices")
    public PageResponse<BillingInvoiceView> invoices(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(billingService.listInvoices(TenantContext.currentTenantId(), PageRequest.of(page, size)));
    }

    @PostMapping("/setup-intent")
    public SetupIntentResponse setupIntent() {
        return billingService.createCardSetupIntent(TenantContext.currentTenantId());
    }

    @PostMapping("/payment-method")
    public void recordPaymentMethod(Authentication authentication, @Valid @RequestBody RecordPaymentMethodRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        billingService.recordPaymentMethod(tenantId, request.paymentMethodId());
        auditLogService.record(authentication, "TENANT_BILLING_PAYMENT_METHOD_UPDATE", "TenantBillingAccount",
                String.valueOf(tenantId), null);
    }
}
