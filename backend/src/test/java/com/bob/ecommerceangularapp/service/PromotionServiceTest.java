package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.PromotionRepository;
import com.bob.ecommerceangularapp.dto.AppliedPromotion;
import com.bob.ecommerceangularapp.dto.PromotionRequest;
import com.bob.ecommerceangularapp.entity.Promotion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (mocked repository) for the automatic promotion selection + discount math, and
 * (roadmap #21, Milestone C) that every read/write is scoped to {@link TenantContext}.
 */
class PromotionServiceTest {

    private static final Long TENANT_ID = 7L;

    private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
    private final PromotionService service = new PromotionService(promotionRepository);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void noActivePromotions_yieldsEmpty() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of());
        assertThat(service.findBest(new BigDecimal("100.00"))).isEmpty();
    }

    @Test
    void percentOff_computesDiscount() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> p.setPercentOff(10))));
        Optional<AppliedPromotion> best = service.findBest(new BigDecimal("100.00"));
        assertThat(best).isPresent();
        assertThat(best.get().discount()).isEqualByComparingTo("10.00");
    }

    @Test
    void amountOff_isCappedAtSubtotal() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> p.setAmountOff(new BigDecimal("50.00")))));
        Optional<AppliedPromotion> best = service.findBest(new BigDecimal("20.00"));
        assertThat(best).isPresent();
        assertThat(best.get().discount()).isEqualByComparingTo("20.00");
    }

    @Test
    void belowMinSpend_isExcluded() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> { p.setAmountOff(new BigDecimal("5.00")); p.setMinSpend(new BigDecimal("50.00")); })));
        assertThat(service.findBest(new BigDecimal("20.00"))).isEmpty();
    }

    @Test
    void beforeStartWindow_isExcluded() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> { p.setPercentOff(10); p.setStartsAt(LocalDate.now().plusDays(1)); })));
        assertThat(service.findBest(new BigDecimal("100.00"))).isEmpty();
    }

    @Test
    void afterEndWindow_isExcluded() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> { p.setPercentOff(10); p.setEndsAt(LocalDate.now().minusDays(1)); })));
        assertThat(service.findBest(new BigDecimal("100.00"))).isEmpty();
    }

    @Test
    void withinWindow_isIncluded() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> {
                    p.setPercentOff(10);
                    p.setStartsAt(LocalDate.now().minusDays(1));
                    p.setEndsAt(LocalDate.now().plusDays(1));
                })));
        assertThat(service.findBest(new BigDecimal("100.00"))).isPresent();
    }

    @Test
    void picksTheHighestValuePromotion() {
        when(promotionRepository.findByActiveTrueAndTenantId(TENANT_ID)).thenReturn(List.of(
                promotion(p -> { p.setName("Small"); p.setAmountOff(new BigDecimal("5.00")); }),
                promotion(p -> { p.setName("Big"); p.setPercentOff(20); })));
        Optional<AppliedPromotion> best = service.findBest(new BigDecimal("100.00"));
        assertThat(best).isPresent();
        assertThat(best.get().name()).isEqualTo("Big");
        assertThat(best.get().discount()).isEqualByComparingTo("20.00");
    }

    @Test
    void findBest_onlyEverReadsTheCurrentTenantsPromotions() {
        service.findBest(new BigDecimal("100.00"));
        verify(promotionRepository).findByActiveTrueAndTenantId(TENANT_ID);
    }

    @Test
    void save_stampsCurrentTenantOntoANewPromotion() {
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        Promotion saved = service.save(new PromotionRequest(null, "New Promo", null, 10, null, null, true, null, null));

        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    private Promotion promotion(Consumer<Promotion> customizer) {
        Promotion p = new Promotion();
        p.setName("Promo");
        p.setActive(true);
        customizer.accept(p);
        return p;
    }
}
