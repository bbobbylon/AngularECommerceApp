package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.GiftCardView;
import com.bob.ecommerceangularapp.service.GiftCardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer-facing: check a gift card's balance before applying it at checkout. */
@RestController
@RequestMapping("/api/checkout/gift-card")
public class GiftCardController {

    private final GiftCardService giftCardService;

    public GiftCardController(GiftCardService giftCardService) {
        this.giftCardService = giftCardService;
    }

    @GetMapping
    public GiftCardView check(@RequestParam String code) {
        return giftCardService.check(code);
    }
}
