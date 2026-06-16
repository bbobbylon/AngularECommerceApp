package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CouponRepository;
import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.entity.Coupon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (mocked repository) for the server-authoritative coupon validation + discount math.
 */
class CouponServiceTest {

    private final CouponRepository couponRepository = mock(CouponRepository.class);
    private final CouponService service = new CouponService(couponRepository);

    @Test
    void blankCode_isRejected() {
        CouponResponse res = service.validate("   ", new BigDecimal("50"));
        assertThat(res.valid()).isFalse();
        assertThat(res.message()).contains("Enter a code");
    }

    @Test
    void unknownCode_isRejected() {
        when(couponRepository.findByCodeIgnoreCase("NOPE")).thenReturn(null);
        assertThat(service.validate("NOPE", new BigDecimal("50")).valid()).isFalse();
    }

    @Test
    void inactiveCode_isRejected() {
        when(couponRepository.findByCodeIgnoreCase("OFF")).thenReturn(coupon(c -> {
            c.setPercentOff(10);
            c.setActive(false);
        }));
        assertThat(service.validate("OFF", new BigDecimal("50")).valid()).isFalse();
    }

    @Test
    void expiredCode_isRejected() {
        when(couponRepository.findByCodeIgnoreCase("OLD")).thenReturn(coupon(c -> {
            c.setPercentOff(10);
            c.setExpiresAt(LocalDate.now().minusDays(1));
        }));
        CouponResponse res = service.validate("OLD", new BigDecimal("50"));
        assertThat(res.valid()).isFalse();
        assertThat(res.message()).contains("expired");
    }

    @Test
    void belowMinSpend_isRejected() {
        when(couponRepository.findByCodeIgnoreCase("SAVE5")).thenReturn(coupon(c -> {
            c.setAmountOff(new BigDecimal("5.00"));
            c.setMinSpend(new BigDecimal("50.00"));
        }));
        CouponResponse res = service.validate("SAVE5", new BigDecimal("20.00"));
        assertThat(res.valid()).isFalse();
        assertThat(res.message()).contains("Spend at least");
    }

    @Test
    void percentOff_computesDiscount() {
        when(couponRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(coupon(c -> c.setPercentOff(10)));
        CouponResponse res = service.validate("WELCOME10", new BigDecimal("100.00"));
        assertThat(res.valid()).isTrue();
        assertThat(res.discount()).isEqualByComparingTo("10.00");
    }

    @Test
    void amountOff_isCappedAtSubtotal() {
        when(couponRepository.findByCodeIgnoreCase("BIG")).thenReturn(coupon(c -> c.setAmountOff(new BigDecimal("50.00"))));
        CouponResponse res = service.validate("BIG", new BigDecimal("20.00"));
        assertThat(res.valid()).isTrue();
        assertThat(res.discount()).isEqualByComparingTo("20.00");
    }

    @Test
    void nullSubtotal_doesNotThrow_andYieldsNoDiscount() {
        when(couponRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(coupon(c -> c.setPercentOff(10)));
        // 10% of an absent (zero) subtotal is 0 -> treated as not applicable, but must not throw.
        assertThat(service.validate("WELCOME10", null).valid()).isFalse();
    }

    private Coupon coupon(Consumer<Coupon> customizer) {
        Coupon c = new Coupon();
        c.setCode("CODE");
        c.setActive(true);
        customizer.accept(c);
        return c;
    }
}
