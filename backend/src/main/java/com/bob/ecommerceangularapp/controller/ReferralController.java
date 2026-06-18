package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ReferralSummary;
import com.bob.ecommerceangularapp.service.ReferralService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer referral standing: their code + rewards earned (assigns a code on first view). */
@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping
    public ReferralSummary summary(@RequestParam String email) {
        return referralService.summary(email);
    }
}
