package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.CouponRepository;
import com.bob.ecommerceangularapp.dto.CouponRequest;
import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.entity.Coupon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /** Validates a code against a subtotal and computes the discount (server-authoritative). */
    public CouponResponse validate(String rawCode, BigDecimal subtotal) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            return CouponResponse.invalid(code, "Enter a code.");
        }
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndTenantId(code, TenantContext.currentTenantId());
        if (coupon == null || !coupon.isActive()) {
            return CouponResponse.invalid(code, "That code isn't valid.");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDate.now())) {
            return CouponResponse.invalid(code, "That code has expired.");
        }
        if (coupon.getMinSpend() != null && subtotal.compareTo(coupon.getMinSpend()) < 0) {
            return CouponResponse.invalid(code,
                    "Spend at least $" + coupon.getMinSpend().setScale(2, RoundingMode.HALF_UP) + " to use this code.");
        }

        BigDecimal discount = discountFor(coupon, subtotal);
        if (discount.signum() <= 0) {
            return CouponResponse.invalid(code, "That code isn't valid.");
        }
        return new CouponResponse(true, coupon.getCode(), coupon.getDescription(), discount,
                "Code applied — you saved $" + discount + "!");
    }

    private BigDecimal discountFor(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getPercentOff() != null && coupon.getPercentOff() > 0) {
            discount = subtotal.multiply(BigDecimal.valueOf(coupon.getPercentOff()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (coupon.getAmountOff() != null) {
            discount = coupon.getAmountOff();
        }
        // never discount more than the subtotal
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    // ----- admin -----

    public List<Coupon> list() {
        return couponRepository.findAllByTenantId(TenantContext.currentTenantId());
    }

    @Transactional
    public Coupon create(CouponRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndTenantId(request.code().trim(), tenantId);
        if (coupon == null) {
            coupon = new Coupon();
            coupon.setTenantId(tenantId);
            coupon.setCode(request.code().trim().toUpperCase());
        }
        coupon.setDescription(request.description());
        coupon.setPercentOff(request.percentOff());
        coupon.setAmountOff(request.amountOff());
        coupon.setMinSpend(request.minSpend());
        coupon.setActive(request.active());
        coupon.setExpiresAt(request.expiresAt());
        return couponRepository.save(coupon);
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = couponRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
        couponRepository.delete(coupon);
    }
}
