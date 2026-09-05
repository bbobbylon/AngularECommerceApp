package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminOrderView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.service.AdminService;
import com.bob.ecommerceangularapp.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin order management: list all orders (newest first) and update fulfillment status. Status
 * updates are also reachable by the RBAC (roadmap #19) {@code OrderManager} role, not just {@code Admin}.
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    public AdminOrderController(AdminService adminService, AuditLogService auditLogService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public PageResponse<AdminOrderView> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(adminService.listOrders(PageRequest.of(page, size)));
    }

    @PutMapping("/{id}/status")
    public AdminOrderView updateStatus(Authentication authentication, @PathVariable Long id, @RequestParam String status) {
        AdminOrderView saved = adminService.updateOrderStatus(id, status);
        auditLogService.record(authentication, "ORDER_STATUS_UPDATE", "Order", String.valueOf(id), "status=" + status);
        return saved;
    }
}
