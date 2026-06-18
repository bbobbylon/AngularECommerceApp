package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.LoyaltySummary;
import com.bob.ecommerceangularapp.service.LoyaltyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer rewards: balance, tier and points history for an email (used by the account + checkout). */
@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping
    public LoyaltySummary summary(@RequestParam String email) {
        return loyaltyService.summary(email);
    }
}
