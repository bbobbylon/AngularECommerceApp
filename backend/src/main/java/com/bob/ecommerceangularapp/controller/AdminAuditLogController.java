package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AuditLogView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only view of the global admin audit log (roadmap #19). */
@RestController
@RequestMapping("/api/admin/audit-log")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public PageResponse<AuditLogView> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String entityType) {
        return PageResponse.of(auditLogService.list(PageRequest.of(page, size), entityType));
    }
}
