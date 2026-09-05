package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.PromotionRequest;
import com.bob.ecommerceangularapp.entity.Promotion;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.PromotionService;
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

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final AuditLogService auditLogService;

    public AdminPromotionController(PromotionService promotionService, AuditLogService auditLogService) {
        this.promotionService = promotionService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<Promotion> list() {
        return promotionService.list();
    }

    @PostMapping
    public ResponseEntity<Promotion> save(Authentication authentication, @Valid @RequestBody PromotionRequest request) {
        boolean isNew = request.id() == null;
        Promotion saved = promotionService.save(request);
        auditLogService.record(authentication, isNew ? "PROMOTION_CREATE" : "PROMOTION_UPDATE",
                "Promotion", String.valueOf(saved.getId()), saved.getName());
        return ResponseEntity.status(isNew ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        promotionService.delete(id);
        auditLogService.record(authentication, "PROMOTION_DELETE", "Promotion", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
