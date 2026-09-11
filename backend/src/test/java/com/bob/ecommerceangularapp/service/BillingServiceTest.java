package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.BillingInvoiceRepository;
import com.bob.ecommerceangularapp.dao.BillingPlanRepository;
import com.bob.ecommerceangularapp.dao.TenantBillingAccountRepository;
import com.bob.ecommerceangularapp.dto.BillingAccountView;
import com.bob.ecommerceangularapp.dto.SetupIntentResponse;
import com.bob.ecommerceangularapp.entity.BillingInvoice;
import com.bob.ecommerceangularapp.entity.BillingPlan;
import com.bob.ecommerceangularapp.entity.TenantBillingAccount;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for tenant billing (roadmap #22). */
class BillingServiceTest {

    private static final Long TENANT_ID = 7L;

    private final TenantBillingAccountRepository accountRepository = mock(TenantBillingAccountRepository.class);
    private final BillingPlanRepository planRepository = mock(BillingPlanRepository.class);
    private final BillingInvoiceRepository invoiceRepository = mock(BillingInvoiceRepository.class);
    // empty key = Stripe not configured
    private final BillingService service =
            new BillingService(accountRepository, planRepository, invoiceRepository, "");

    private BillingPlan activePlan(Long id) {
        BillingPlan plan = new BillingPlan();
        plan.setId(id);
        plan.setName("Starter");
        plan.setMonthlyPrice(new BigDecimal("19.99"));
        plan.setCurrency("USD");
        plan.setActive(true);
        return plan;
    }

    private TenantBillingAccount account(Long tenantId, String status) {
        TenantBillingAccount account = new TenantBillingAccount();
        account.setId(1L);
        account.setTenantId(tenantId);
        account.setStatus(status);
        return account;
    }

    // ---- assignPlan ----

    @Test
    void assignPlan_createsAccountWhenAbsent() {
        when(accountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(planRepository.findById(10L)).thenReturn(Optional.of(activePlan(10L)));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignPlan(TENANT_ID, 10L);

        verify(accountRepository).save(argThatAccount(a -> a.getTenantId().equals(TENANT_ID)
                && a.getPlanId().equals(10L) && "ACTIVE".equals(a.getStatus()) && a.getCurrentPeriodEnd() != null));
    }

    @Test
    void assignPlan_updatesExistingAndResetsPeriod() {
        TenantBillingAccount existing = account(TENANT_ID, "PAST_DUE");
        existing.setPlanId(5L);
        existing.setCurrentPeriodEnd(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
        when(accountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(existing));
        when(planRepository.findById(10L)).thenReturn(Optional.of(activePlan(10L)));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignPlan(TENANT_ID, 10L);

        assertThat(existing.getPlanId()).isEqualTo(10L);
        assertThat(existing.getStatus()).isEqualTo("ACTIVE");
        assertThat(existing.getCurrentPeriodEnd()).isAfter(new Date());
    }

    @Test
    void assignPlan_nullPlanIdUnassigns() {
        TenantBillingAccount existing = account(TENANT_ID, "ACTIVE");
        existing.setPlanId(5L);
        when(accountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignPlan(TENANT_ID, null);

        assertThat(existing.getPlanId()).isNull();
        assertThat(existing.getStatus()).isEqualTo("NO_PLAN");
    }

    @Test
    void assignPlan_rejectsAnInactivePlan() {
        BillingPlan inactive = activePlan(10L);
        inactive.setActive(false);
        when(accountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(planRepository.findById(10L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.assignPlan(TENANT_ID, 10L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignPlan_unknownPlanThrows() {
        when(accountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(planRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignPlan(TENANT_ID, 10L)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---- getAccount ----

    @Test
    void getAccount_returnsNoPlanViewWhenAbsent() {
        when(accountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        BillingAccountView view = service.getAccount(TENANT_ID);

        assertThat(view.status()).isEqualTo("NO_PLAN");
        assertThat(view.planName()).isNull();
    }

    // ---- card management, no Stripe configured ----

    @Test
    void createCardSetupIntent_disabledWithoutStripe() {
        SetupIntentResponse response = service.createCardSetupIntent(TENANT_ID);

        assertThat(response.enabled()).isFalse();
        assertThat(response.clientSecret()).isNull();
    }

    @Test
    void recordPaymentMethod_throwsWithoutStripe() {
        assertThatThrownBy(() -> service.recordPaymentMethod(TENANT_ID, "pm_123"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- chargeDueAccounts ----

    @Test
    void chargeDueAccounts_skipsGracefullyWithoutStripeAndRecordsSkipped() {
        TenantBillingAccount due = account(TENANT_ID, "ACTIVE");
        due.setPlanId(10L);
        when(accountRepository.findAllByStatusInAndCurrentPeriodEndLessThanEqual(anyList(), any(Date.class)))
                .thenReturn(List.of(due));
        when(planRepository.findById(10L)).thenReturn(Optional.of(activePlan(10L)));

        int count = service.chargeDueAccounts();

        assertThat(count).isEqualTo(1);
        verify(invoiceRepository).save(argThatInvoice(inv -> "SKIPPED".equals(inv.getStatus())));
        // status/currentPeriodEnd untouched so it retries automatically
        verify(accountRepository, never()).save(any());
    }

    @Test
    void chargeDueAccounts_ignoresAccountsWithNoPlan() {
        TenantBillingAccount noPlan = account(TENANT_ID, "ACTIVE");
        // planId left null
        when(accountRepository.findAllByStatusInAndCurrentPeriodEndLessThanEqual(anyList(), any(Date.class)))
                .thenReturn(List.of(noPlan));

        int count = service.chargeDueAccounts();

        assertThat(count).isEqualTo(1);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void chargeDueAccounts_marksPastDueWithNoCardOnFile_whenStripeIsConfigured() {
        BillingInvoiceRepository invoiceRepo = mock(BillingInvoiceRepository.class);
        TenantBillingAccountRepository accountRepo = mock(TenantBillingAccountRepository.class);
        BillingPlanRepository planRepo = mock(BillingPlanRepository.class);
        BillingService configuredService = new BillingService(accountRepo, planRepo, invoiceRepo, "sk_test_dummy");

        TenantBillingAccount due = account(TENANT_ID, "ACTIVE");
        due.setPlanId(10L);
        // no stripePaymentMethodId on file
        when(accountRepo.findAllByStatusInAndCurrentPeriodEndLessThanEqual(anyList(), any(Date.class)))
                .thenReturn(List.of(due));
        when(planRepo.findById(10L)).thenReturn(Optional.of(activePlan(10L)));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        configuredService.chargeDueAccounts();

        assertThat(due.getStatus()).isEqualTo("PAST_DUE");
        verify(invoiceRepo).save(argThatInvoice(inv -> "FAILED".equals(inv.getStatus())));
    }

    @Test
    void chargeDueAccounts_returnsZeroWhenNothingIsDue() {
        when(accountRepository.findAllByStatusInAndCurrentPeriodEndLessThanEqual(anyList(), any(Date.class)))
                .thenReturn(List.of());

        int count = service.chargeDueAccounts();

        assertThat(count).isEqualTo(0);
        verify(invoiceRepository, never()).save(any());
    }

    private static TenantBillingAccount argThatAccount(java.util.function.Predicate<TenantBillingAccount> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }

    private static BillingInvoice argThatInvoice(java.util.function.Predicate<BillingInvoice> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
