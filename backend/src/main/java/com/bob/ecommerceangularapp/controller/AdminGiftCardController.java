package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminGiftCardRequest;
import com.bob.ecommerceangularapp.entity.GiftCard;
import com.bob.ecommerceangularapp.service.GiftCardService;
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

/** Admin gift-card management: issue, list, and deactivate cards. */
@RestController
@RequestMapping("/api/admin/gift-cards")
public class AdminGiftCardController {

    private final GiftCardService giftCardService;

    public AdminGiftCardController(GiftCardService giftCardService) {
        this.giftCardService = giftCardService;
    }

    @GetMapping
    public List<GiftCard> list() {
        return giftCardService.listAll();
    }

    @PostMapping
    public ResponseEntity<GiftCard> issue(@Valid @RequestBody AdminGiftCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(giftCardService.issue(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        giftCardService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
