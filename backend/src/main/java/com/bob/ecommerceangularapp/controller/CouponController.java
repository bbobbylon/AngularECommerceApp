package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.dto.CouponValidateRequest;
import com.bob.ecommerceangularapp.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public CouponResponse validate(@Valid @RequestBody CouponValidateRequest request) {
        return couponService.validate(request.code(), request.subtotal());
    }
}
