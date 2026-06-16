package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.SystemHealth;
import com.bob.ecommerceangularapp.service.SystemHealthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Friendly system-health view for the admin back-office (gated like the rest of {@code /api/admin/**}).
 * Surfaces integration readiness, build version, and uptime — a digestible companion to the raw
 * Actuator endpoints (which stay terse for unauthenticated callers).
 */
@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
@RestController
@RequestMapping("/api/admin/system")
public class AdminSystemController {

    private final SystemHealthService systemHealthService;

    public AdminSystemController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping
    public SystemHealth system() {
        return systemHealthService.current();
    }
}
