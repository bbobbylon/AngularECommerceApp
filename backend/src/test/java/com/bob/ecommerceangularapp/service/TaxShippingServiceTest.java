package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ShippingMethodRepository;
import com.bob.ecommerceangularapp.dao.TaxRateRepository;
import com.bob.ecommerceangularapp.dto.AppliedPromotion;
import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.dto.QuoteRequest;
import com.bob.ecommerceangularapp.dto.QuoteResponse;
import com.bob.ecommerceangularapp.dto.TaxRateRequest;
import com.bob.ecommerceangularapp.entity.ShippingMethod;
import com.bob.ecommerceangularapp.entity.TaxRate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring/DB) for the tax + shipping quote computation, and (roadmap #21,
 * Milestone C) that every read/write is scoped to {@link TenantContext}.
 */
class TaxShippingServiceTest {

    private static final Long TENANT_ID = 7L;

    private final TaxRateRepository taxRateRepository = mock(TaxRateRepository.class);
    private final ShippingMethodRepository shippingMethodRepository = mock(ShippingMethodRepository.class);
    private final CouponService couponService = mock(CouponService.class);
    private final PromotionService promotionService = mock(PromotionService.class);
    private final TaxShippingService service =
            new TaxShippingService(taxRateRepository, shippingMethodRepository, couponService, promotionService);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private ShippingMethod standard() {
        return ShippingMethod.builder().code("STANDARD").name("Standard").baseRate(new BigDecimal("5.99"))
                .freeOverThreshold(new BigDecimal("50.00")).active(true).build();
    }

    @Test
    void quote_appliesStateTaxAndFlatShippingBelowThreshold() {
        when(shippingMethodRepository.findByCodeAndTenantId("STANDARD", TENANT_ID)).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                new TaxRate(1L, TENANT_ID, "United States", "California", new BigDecimal("7.25"), true)));

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("40.00"), "United States", "California", null, "STANDARD"));

        assertThat(q.shippingAmount()).isEqualByComparingTo("5.99");   // below $50 → charged
        assertThat(q.taxAmount()).isEqualByComparingTo("2.90");        // 40 * 7.25%
        assertThat(q.total()).isEqualByComparingTo("48.89");           // 40 + 5.99 + 2.90
    }

    @Test
    void quote_freeShippingOverThresholdAndCountryWideFallback() {
        when(shippingMethodRepository.findByCodeAndTenantId("STANDARD", TENANT_ID)).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                new TaxRate(1L, TENANT_ID, "United States", null, new BigDecimal("5.00"), true))); // country-wide

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("60.00"), "United States", "Nowhere", null, "STANDARD"));

        assertThat(q.shippingAmount()).isEqualByComparingTo("0.00");   // ≥ $50 → free
        assertThat(q.taxAmount()).isEqualByComparingTo("3.00");        // 60 * 5% (country-wide fallback)
        assertThat(q.total()).isEqualByComparingTo("63.00");
    }

    @Test
    void quote_unknownRegionIsTaxFree() {
        when(shippingMethodRepository.findByCodeAndTenantId("STANDARD", TENANT_ID)).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of());

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("30.00"), "Narnia", "West", null, "STANDARD"));

        assertThat(q.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(q.total()).isEqualByComparingTo("35.99");
    }

    @Test
    void quote_subtractsValidatedCouponBeforeTax() {
        when(shippingMethodRepository.findByCodeAndTenantId("STANDARD", TENANT_ID)).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                new TaxRate(1L, TENANT_ID, "United States", "California", new BigDecimal("10.00"), true)));
        when(couponService.validate(anyString(), any())).thenReturn(
                new CouponResponse(true, "SAVE10", "$10 off", new BigDecimal("10.00"), "ok"));

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("40.00"), "United States", "California", "SAVE10", "STANDARD"));

        assertThat(q.discount()).isEqualByComparingTo("10.00");
        assertThat(q.taxAmount()).isEqualByComparingTo("3.00");        // (40 - 10) * 10%
        assertThat(q.total()).isEqualByComparingTo("38.99");           // 30 + 5.99 + 3.00
    }

    @Test
    void quote_stacksAutomaticPromotionOnTopOfCoupon() {
        when(shippingMethodRepository.findByCodeAndTenantId("STANDARD", TENANT_ID)).thenReturn(Optional.of(standard()));
        when(taxRateRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                new TaxRate(1L, TENANT_ID, "United States", "California", new BigDecimal("10.00"), true)));
        when(couponService.validate(anyString(), any())).thenReturn(
                new CouponResponse(true, "SAVE10", "$10 off", new BigDecimal("10.00"), "ok"));
        when(promotionService.findBest(any())).thenReturn(
                Optional.of(new AppliedPromotion("Summer Sale", new BigDecimal("5.00"))));

        QuoteResponse q = service.quote(new QuoteRequest(
                new BigDecimal("40.00"), "United States", "California", "SAVE10", "STANDARD"));

        assertThat(q.discount()).isEqualByComparingTo("10.00");
        assertThat(q.promotionName()).isEqualTo("Summer Sale");
        assertThat(q.promotionDiscount()).isEqualByComparingTo("5.00");
        assertThat(q.taxAmount()).isEqualByComparingTo("2.50");        // (40 - 10 - 5) * 10%
        assertThat(q.total()).isEqualByComparingTo("33.49");           // 25 + 5.99 + 2.50
    }

    @Test
    void listShippingMethods_onlyEverReadsTheCurrentTenantsMethods() {
        when(shippingMethodRepository.findByActiveTrueAndTenantIdOrderBySortOrderAscIdAsc(TENANT_ID)).thenReturn(List.of());
        service.listShippingMethods();
        verify(shippingMethodRepository).findByActiveTrueAndTenantIdOrderBySortOrderAscIdAsc(TENANT_ID);
    }

    @Test
    void saveTaxRate_stampsCurrentTenantOntoANewRate() {
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxRate saved = service.saveTaxRate(new TaxRateRequest(null, "Canada", null, new BigDecimal("5.00"), true));

        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }
}
