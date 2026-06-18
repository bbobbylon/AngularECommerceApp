package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ShippingMethodRequest;
import com.bob.ecommerceangularapp.dto.TaxRateRequest;
import com.bob.ecommerceangularapp.entity.ShippingMethod;
import com.bob.ecommerceangularapp.entity.TaxRate;
import com.bob.ecommerceangularapp.service.TaxShippingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public AdminTaxShippingController(TaxShippingService taxShippingService) {
        this.taxShippingService = taxShippingService;
    }

    // ----- tax rates -----

    @GetMapping("/tax-rates")
    public List<TaxRate> listTaxRates() {
        return taxShippingService.listTaxRates();
    }

    @PostMapping("/tax-rates")
    public TaxRate saveTaxRate(@Valid @RequestBody TaxRateRequest request) {
        return taxShippingService.saveTaxRate(request);
    }

    @DeleteMapping("/tax-rates/{id}")
    public ResponseEntity<Void> deleteTaxRate(@PathVariable Long id) {
        taxShippingService.deleteTaxRate(id);
        return ResponseEntity.noContent().build();
    }

    // ----- shipping methods -----

    @GetMapping("/shipping-methods")
    public List<ShippingMethod> listShippingMethods() {
        return taxShippingService.listAllShippingMethods();
    }

    @PostMapping("/shipping-methods")
    public ResponseEntity<ShippingMethod> saveShippingMethod(@Valid @RequestBody ShippingMethodRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(taxShippingService.saveShippingMethod(request));
    }

    @DeleteMapping("/shipping-methods/{id}")
    public ResponseEntity<Void> deleteShippingMethod(@PathVariable Long id) {
        taxShippingService.deleteShippingMethod(id);
        return ResponseEntity.noContent().build();
    }
}
