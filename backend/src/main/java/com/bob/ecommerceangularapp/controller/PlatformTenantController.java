package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.PlatformTenantRequest;
import com.bob.ecommerceangularapp.entity.Tenant;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.PlatformTenantService;
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
 * Platform-level tenant management (roadmap #21, Milestone B) — {@code SuperAdmin}-only, gated in
 * {@code SecurityConfig} and excluded from tenant resolution/guarding in
 * {@code TenantResolutionFilter} (a tenant isn't scoped to itself).
 */
@RestController
@RequestMapping("/api/platform/tenants")
public class PlatformTenantController {

    private final PlatformTenantService platformTenantService;
    private final AuditLogService auditLogService;

    public PlatformTenantController(PlatformTenantService platformTenantService, AuditLogService auditLogService) {
        this.platformTenantService = platformTenantService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<Tenant> list() {
        return platformTenantService.list();
    }

    @PostMapping
    public ResponseEntity<Tenant> save(Authentication authentication, @Valid @RequestBody PlatformTenantRequest request) {
        boolean isCreate = request.id() == null;
        Tenant saved = platformTenantService.save(request);
        auditLogService.record(authentication, isCreate ? "PLATFORM_TENANT_CREATE" : "PLATFORM_TENANT_UPDATE",
                "Tenant", String.valueOf(saved.getId()), saved.getSlug());
        return ResponseEntity.status(isCreate ? HttpStatus.CREATED : HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(Authentication authentication, @PathVariable Long id) {
        platformTenantService.deactivate(id);
        auditLogService.record(authentication, "PLATFORM_TENANT_DEACTIVATE", "Tenant", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
