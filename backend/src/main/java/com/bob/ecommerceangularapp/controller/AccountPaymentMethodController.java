package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.RecordPaymentMethodRequest;
import com.bob.ecommerceangularapp.dto.SetupIntentResponse;
import com.bob.ecommerceangularapp.entity.SavedPaymentMethod;
import com.bob.ecommerceangularapp.service.PaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Saved cards (gated under /api/account). Add uses a Stripe SetupIntent; degrades without Stripe. */
@RestController
@RequestMapping("/api/account/payment-methods")
public class AccountPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public AccountPaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public List<SavedPaymentMethod> list(@RequestParam String email) {
        return paymentMethodService.list(email);
    }

    @PostMapping("/setup-intent")
    public SetupIntentResponse setupIntent(@RequestParam String email) {
        return paymentMethodService.createSetupIntent(email);
    }

    @PostMapping
    public SavedPaymentMethod record(@RequestParam String email, @Valid @RequestBody RecordPaymentMethodRequest request) {
        return paymentMethodService.record(email, request.paymentMethodId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestParam String email, @PathVariable Long id) {
        paymentMethodService.delete(email, id);
        return ResponseEntity.noContent().build();
    }
}
