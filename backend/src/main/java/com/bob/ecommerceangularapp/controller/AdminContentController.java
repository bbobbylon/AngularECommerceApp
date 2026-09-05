package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.FaqEntryRequest;
import com.bob.ecommerceangularapp.dto.SiteBannerRequest;
import com.bob.ecommerceangularapp.entity.FaqEntry;
import com.bob.ecommerceangularapp.entity.SiteBanner;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private final ContentService contentService;
    private final AuditLogService auditLogService;

    public AdminContentController(ContentService contentService, AuditLogService auditLogService) {
        this.contentService = contentService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/banner")
    public SiteBanner banner() {
        return contentService.getBannerForAdmin();
    }

    @PutMapping("/banner")
    public SiteBanner saveBanner(Authentication authentication, @Valid @RequestBody SiteBannerRequest request) {
        SiteBanner saved = contentService.saveBanner(request);
        auditLogService.record(authentication, "BANNER_UPDATE", "SiteBanner", String.valueOf(saved.getId()), saved.getMessage());
        return saved;
    }

    @GetMapping("/faq")
    public List<FaqEntry> listFaq() {
        return contentService.listFaqForAdmin();
    }

    @PostMapping("/faq")
    public ResponseEntity<FaqEntry> saveFaq(Authentication authentication, @Valid @RequestBody FaqEntryRequest request) {
        boolean isNew = request.id() == null;
        FaqEntry saved = contentService.saveFaq(request);
        auditLogService.record(authentication, isNew ? "FAQ_CREATE" : "FAQ_UPDATE", "FaqEntry", String.valueOf(saved.getId()), saved.getQuestion());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/faq/{id}")
    public ResponseEntity<Void> deleteFaq(Authentication authentication, @PathVariable Long id) {
        contentService.deleteFaq(id);
        auditLogService.record(authentication, "FAQ_DELETE", "FaqEntry", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
