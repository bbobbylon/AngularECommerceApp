package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminGiftCardRequest;
import com.bob.ecommerceangularapp.entity.GiftCard;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.GiftCardService;
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

/** Admin gift-card management: issue, list, and deactivate cards. */
@RestController
@RequestMapping("/api/admin/gift-cards")
public class AdminGiftCardController {

    private final GiftCardService giftCardService;
    private final AuditLogService auditLogService;

    public AdminGiftCardController(GiftCardService giftCardService, AuditLogService auditLogService) {
        this.giftCardService = giftCardService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<GiftCard> list() {
        return giftCardService.listAll();
    }

    @PostMapping
    public ResponseEntity<GiftCard> issue(Authentication authentication, @Valid @RequestBody AdminGiftCardRequest request) {
        GiftCard saved = giftCardService.issue(request);
        auditLogService.record(authentication, "GIFT_CARD_ISSUE", "GiftCard", String.valueOf(saved.getId()), saved.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(Authentication authentication, @PathVariable Long id) {
        giftCardService.deactivate(id);
        auditLogService.record(authentication, "GIFT_CARD_DEACTIVATE", "GiftCard", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
