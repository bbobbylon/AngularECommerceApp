package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminApiKeyRequest;
import com.bob.ecommerceangularapp.dto.ApiKeyCreatedView;
import com.bob.ecommerceangularapp.dto.ApiKeyView;
import com.bob.ecommerceangularapp.service.ApiKeyService;
import com.bob.ecommerceangularapp.service.AuditLogService;
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

/**
 * Admin API-key management: issue, list, and revoke keys for headless/programmatic access to this
 * tenant's own back office (roadmap #23, Milestone A). Unlike the other admin resources, {@code list}
 * returns {@link ApiKeyView} rather than the raw entity — the entity's {@code keyHash} is a secret
 * derivative that has no reason to ever leave the server, even hashed.
 */
@RestController
@RequestMapping("/api/admin/api-keys")
public class AdminApiKeyController {

    private final ApiKeyService apiKeyService;
    private final AuditLogService auditLogService;

    public AdminApiKeyController(ApiKeyService apiKeyService, AuditLogService auditLogService) {
        this.apiKeyService = apiKeyService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<ApiKeyView> list() {
        return apiKeyService.listAll().stream().map(ApiKeyView::from).toList();
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreatedView> issue(Authentication authentication, @Valid @RequestBody AdminApiKeyRequest request) {
        ApiKeyCreatedView created = apiKeyService.issue(request);
        auditLogService.record(authentication, "API_KEY_ISSUE", "ApiKey", String.valueOf(created.id()), created.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(Authentication authentication, @PathVariable Long id) {
        apiKeyService.revoke(id);
        auditLogService.record(authentication, "API_KEY_REVOKE", "ApiKey", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
