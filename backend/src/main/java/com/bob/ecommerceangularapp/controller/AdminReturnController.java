package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ReturnDecisionRequest;
import com.bob.ecommerceangularapp.dto.ReturnRequestView;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin returns queue: review and approve/deny (approval issues a Stripe refund when possible).
 * Decisions are also reachable by the RBAC (roadmap #19) {@code OrderManager} role, not just {@code Admin}.
 */
@RestController
@RequestMapping("/api/admin/returns")
public class AdminReturnController {

    private final ReturnService returnService;
    private final AuditLogService auditLogService;

    public AdminReturnController(ReturnService returnService, AuditLogService auditLogService) {
        this.returnService = returnService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<ReturnRequestView> list() {
        return returnService.adminList();
    }

    @PutMapping("/{id}/decision")
    public ReturnRequestView decide(Authentication authentication, @PathVariable Long id, @Valid @RequestBody ReturnDecisionRequest decision) {
        ReturnRequestView saved = returnService.decide(id, decision);
        auditLogService.record(authentication, "RETURN_DECISION", "ReturnRequest", String.valueOf(id), "action=" + decision.action());
        return saved;
    }
}
