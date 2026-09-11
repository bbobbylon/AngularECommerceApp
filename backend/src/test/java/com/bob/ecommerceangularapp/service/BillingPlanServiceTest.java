package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.BillingPlanRepository;
import com.bob.ecommerceangularapp.dto.PlatformBillingPlanRequest;
import com.bob.ecommerceangularapp.entity.BillingPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for the platform billing-plan catalog (roadmap #22). */
class BillingPlanServiceTest {

    private final BillingPlanRepository repo = mock(BillingPlanRepository.class);
    private final BillingPlanService service = new BillingPlanService(repo);

    private BillingPlan plan(Long id, String name) {
        BillingPlan p = new BillingPlan();
        p.setId(id);
        p.setName(name);
        p.setMonthlyPrice(new BigDecimal("19.99"));
        p.setCurrency("USD");
        p.setActive(true);
        return p;
    }

    @Test
    void save_createAssignsDefaultsAndPersists() {
        when(repo.existsByName("Starter")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            BillingPlan p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        BillingPlan saved = service.save(new PlatformBillingPlanRequest(
                null, "Starter", new BigDecimal("19.99"), null, "Feature A\nFeature B", null, null));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getName()).isEqualTo("Starter");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getSortOrder()).isEqualTo(0);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void save_createRejectsADuplicateName() {
        when(repo.existsByName("Starter")).thenReturn(true);

        assertThatThrownBy(() -> service.save(new PlatformBillingPlanRequest(
                null, "Starter", new BigDecimal("19.99"), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_updatePreservesIdAndAllowsAnUnchangedNameOnSelfUpdate() {
        BillingPlan existing = plan(3L, "Starter");
        when(repo.existsByNameAndIdNot("Starter", 3L)).thenReturn(false);
        when(repo.findById(3L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BillingPlan saved = service.save(new PlatformBillingPlanRequest(
                3L, "Starter", new BigDecimal("29.99"), "usd", null, 2, false));

        assertThat(saved.getId()).isEqualTo(3L);
        assertThat(saved.getMonthlyPrice()).isEqualByComparingTo("29.99");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getSortOrder()).isEqualTo(2);
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void save_updateRejectsANameAlreadyUsedByADifferentPlan() {
        when(repo.existsByNameAndIdNot("Taken", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.save(new PlatformBillingPlanRequest(
                3L, "Taken", new BigDecimal("9.99"), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_updateUnknownIdThrows() {
        when(repo.existsByNameAndIdNot("Starter", 99L)).thenReturn(false);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(new PlatformBillingPlanRequest(
                99L, "Starter", new BigDecimal("9.99"), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivate_setsActiveFalse() {
        BillingPlan existing = plan(3L, "Starter");
        when(repo.findById(3L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(3L);

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void deactivate_unknownIdThrows() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
