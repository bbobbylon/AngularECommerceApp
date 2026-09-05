package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.CouponRequest;
import com.bob.ecommerceangularapp.entity.Coupon;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.CouponService;
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
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;
    private final AuditLogService auditLogService;

    public AdminCouponController(CouponService couponService, AuditLogService auditLogService) {
        this.couponService = couponService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<Coupon> list() {
        return couponService.list();
    }

    @PostMapping
    public ResponseEntity<Coupon> create(Authentication authentication, @Valid @RequestBody CouponRequest request) {
        Coupon saved = couponService.create(request);
        auditLogService.record(authentication, "COUPON_CREATE", "Coupon", String.valueOf(saved.getId()), saved.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        couponService.delete(id);
        auditLogService.record(authentication, "COUPON_DELETE", "Coupon", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}
