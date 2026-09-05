package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ShippingMethodRequest;
import com.bob.ecommerceangularapp.dto.TaxRateRequest;
import com.bob.ecommerceangularapp.entity.ShippingMethod;
import com.bob.ecommerceangularapp.entity.TaxRate;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.TaxShippingService;
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

/** Admin config for tax rates + shipping methods (gated under /api/admin like the rest of the back-office). */
@RestController
@RequestMapping("/api/admin")
public class AdminTaxShippingController {

    private final TaxShippingService taxShippingService;
    private final AuditLogService auditLogService;

    public AdminTaxShippingController(TaxShippingService taxShippingService, AuditLogService auditLogService) {
        this.taxShippingService = taxShippingService;
        this.auditLogService = auditLogService;
    }

    // ----- tax rates -----

    @GetMapping("/tax-rates")
    public List<TaxRate> listTaxRates() {
        return taxShippingService.listTaxRates();
    }

    @PostMapping("/tax-rates")
    public TaxRate saveTaxRate(Authentication authentication, @Valid @RequestBody TaxRateRequest request) {
        boolean isNew = request.id() == null;
        TaxRate saved = taxShippingService.saveTaxRate(request);
        auditLogService.record(authentication, isNew ? "TAX_RATE_CREATE" : "TAX_RATE_UPDATE",
                "TaxRate", String.valueOf(saved.getId()), null);
        return saved;
    }

    @DeleteMapping("/tax-rates/{id}")
    public ResponseEntity<Void> deleteTaxRate(Authentication authentication, @PathVariable Long id) {
        taxShippingService.deleteTaxRate(id);
        auditLogService.record(authentication, "TAX_RATE_DELETE", "TaxRate", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }

    // ----- shipping methods -----

    @GetMapping("/shipping-methods")
    public List<ShippingMethod> listShippingMethods() {
        return taxShippingService.listAllShippingMethods();
    }

    @PostMapping("/shipping-methods")
    public ResponseEntity<ShippingMethod> saveShippingMethod(Authentication authentication, @Valid @RequestBody ShippingMethodRequest request) {
        boolean isNew = request.id() == null;
        ShippingMethod saved = taxShippingService.saveShippingMethod(request);
        auditLogService.record(authentication, isNew ? "SHIPPING_METHOD_CREATE" : "SHIPPING_METHOD_UPDATE",
                "ShippingMethod", String.valueOf(saved.getId()), saved.getName());
        return ResponseEntity.status(HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/shipping-methods/{id}")
    public ResponseEntity<Void> deleteShippingMethod(Authentication authentication, @PathVariable Long id) {
        taxShippingService.deleteShippingMethod(id);
        auditLogService.record(authentication, "SHIPPING_METHOD_DELETE", "ShippingMethod", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
