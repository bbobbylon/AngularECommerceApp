package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminOrderView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin order management: list all orders (newest first) and update fulfillment status. */
@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminService adminService;

    public AdminOrderController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public PageResponse<AdminOrderView> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(adminService.listOrders(PageRequest.of(page, size)));
    }

    @PutMapping("/{id}/status")
    public AdminOrderView updateStatus(@PathVariable Long id, @RequestParam String status) {
        return adminService.updateOrderStatus(id, status);
    }
}
