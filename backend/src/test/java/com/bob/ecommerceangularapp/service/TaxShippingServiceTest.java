package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ShippingMethodRepository;
import com.bob.ecommerceangularapp.dao.TaxRateRepository;
import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.dto.QuoteRequest;
import com.bob.ecommerceangularapp.dto.QuoteResponse;
import com.bob.ecommerceangularapp.entity.ShippingMethod;
import com.bob.ecommerceangularapp.entity.TaxRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for the tax + shipping quote computation. */
class TaxShippingServiceTest {

    private final TaxRateRepository taxRateRepository = mock(TaxRateRepository.class);
    private final ShippingMethodRepository shippingMethodRepository = mock(ShippingMethodRepository.class);
    private final CouponService couponService = mock(CouponService.class);
    private final TaxShippingService service =
            new TaxShippingService(taxRateRepository, shippingMethodRepository, couponService);

    private ShippingMethod standard() {
        return ShippingMethod.builder().code("STANDARD").name("Standard").baseRate(new BigDecimal("5.99"))
                .freeOverThreshold(new BigDecimal("50.00")).active(true).build();
    }

    @Test
    void quote_appliesStateTaxAndFlatShippingBelowThreshold() {
        when(shippingMethodRepository.findByCode("STANDARD")).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrue()).thenReturn(List.of(
                new TaxRate(1L, "United States", "California", new BigDecimal("7.25"), true)));

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("40.00"), "United States", "California", null, "STANDARD"));

        assertThat(q.shippingAmount()).isEqualByComparingTo("5.99");   // below $50 → charged
        assertThat(q.taxAmount()).isEqualByComparingTo("2.90");        // 40 * 7.25%
        assertThat(q.total()).isEqualByComparingTo("48.89");           // 40 + 5.99 + 2.90
    }

    @Test
    void quote_freeShippingOverThresholdAndCountryWideFallback() {
        when(shippingMethodRepository.findByCode("STANDARD")).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrue()).thenReturn(List.of(
                new TaxRate(1L, "United States", null, new BigDecimal("5.00"), true))); // country-wide

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("60.00"), "United States", "Nowhere", null, "STANDARD"));

        assertThat(q.shippingAmount()).isEqualByComparingTo("0.00");   // ≥ $50 → free
        assertThat(q.taxAmount()).isEqualByComparingTo("3.00");        // 60 * 5% (country-wide fallback)
        assertThat(q.total()).isEqualByComparingTo("63.00");
    }

    @Test
    void quote_unknownRegionIsTaxFree() {
        when(shippingMethodRepository.findByCode("STANDARD")).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrue()).thenReturn(List.of());

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("30.00"), "Narnia", "West", null, "STANDARD"));

        assertThat(q.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(q.total()).isEqualByComparingTo("35.99");
    }

    @Test
    void quote_subtractsValidatedCouponBeforeTax() {
        when(shippingMethodRepository.findByCode("STANDARD")).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrue()).thenReturn(List.of(
                new TaxRate(1L, "United States", "California", new BigDecimal("10.00"), true)));
        when(couponService.validate(anyString(), any())).thenReturn(
                new CouponResponse(true, "SAVE10", "$10 off", new BigDecimal("10.00"), "ok"));

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("40.00"), "United States", "California", "SAVE10", "STANDARD"));

        assertThat(q.discount()).isEqualByComparingTo("10.00");
        assertThat(q.taxAmount()).isEqualByComparingTo("3.00");        // (40 - 10) * 10%
        assertThat(q.total()).isEqualByComparingTo("38.99");           // 30 + 5.99 + 3.00
    }
}
