package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.BillingInvoiceRepository;
import com.bob.ecommerceangularapp.dao.BillingPlanRepository;
import com.bob.ecommerceangularapp.dao.TenantBillingAccountRepository;
import com.bob.ecommerceangularapp.dto.BillingAccountView;
import com.bob.ecommerceangularapp.dto.BillingInvoiceView;
import com.bob.ecommerceangularapp.dto.SetupIntentResponse;
import com.bob.ecommerceangularapp.entity.BillingInvoice;
import com.bob.ecommerceangularapp.entity.BillingPlan;
import com.bob.ecommerceangularapp.entity.TenantBillingAccount;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenant billing (roadmap #22): assigning a tenant to a {@link BillingPlan}, managing the card on
 * file, and the actual recurring charge. Deliberately does <b>not</b> create a Stripe Customer or
 * Subscription object — plan/status/renewal date are this app's own bookkeeping, recomputed here and
 * never read back from Stripe, and a saved card is just a bare Stripe {@code PaymentMethod} id reused
 * directly (identical shape to {@link PaymentMethodService}, which already established this
 * codebase's "no Stripe Customer object" precedent). This also means no webhook is needed:
 * {@link #chargeDueAccounts()} is driven entirely by a scheduled sweep of this app's own tables, not
 * by Stripe pushing events back.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final List<String> DUE_STATUSES = List.of("ACTIVE", "PAST_DUE");

    private final TenantBillingAccountRepository accountRepository;
    private final BillingPlanRepository planRepository;
    private final BillingInvoiceRepository invoiceRepository;
    private final String stripeKey;

    public BillingService(TenantBillingAccountRepository accountRepository,
                          BillingPlanRepository planRepository,
                          BillingInvoiceRepository invoiceRepository,
                          @Value("${stripe.key.secret}") String stripeKey) {
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.invoiceRepository = invoiceRepository;
        this.stripeKey = stripeKey;
    }

    private boolean stripeConfigured() {
        return stripeKey != null && !stripeKey.isBlank();
    }

    // ---- Platform: plan assignment (roadmap #22 decision: SuperAdmin-only) ----

    /** Assigns (or, with a null {@code planId}, unassigns) a tenant's plan; creates the account row lazily. */
    @Transactional
    public void assignPlan(Long tenantId, Long planId) {
        TenantBillingAccount account = accountRepository.findByTenantId(tenantId).orElseGet(() -> newAccountFor(tenantId));
        if (planId == null) {
            account.setPlanId(null);
            account.setStatus("NO_PLAN");
            accountRepository.save(account);
            return;
        }
        BillingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Billing plan not found: " + planId));
        if (!plan.isActive()) {
            throw new IllegalArgumentException("Billing plan \"" + plan.getName() + "\" is not active.");
        }
        account.setPlanId(planId);
        account.setStatus("ACTIVE");
        account.setCurrentPeriodEnd(oneMonthFrom(new Date()));
        accountRepository.save(account);
    }

    private static TenantBillingAccount newAccountFor(Long tenantId) {
        TenantBillingAccount account = new TenantBillingAccount();
        account.setTenantId(tenantId);
        return account;
    }

    // ---- Tenant-facing reads ----

    @Transactional(readOnly = true)
    public BillingAccountView getAccount(Long tenantId) {
        return accountRepository.findByTenantId(tenantId).map(this::toView).orElseGet(BillingAccountView::noPlan);
    }

    @Transactional(readOnly = true)
    public Page<BillingInvoiceView> listInvoices(Long tenantId, Pageable pageable) {
        return invoiceRepository.findAllByTenantIdOrderByAttemptedAtDesc(tenantId, pageable).map(BillingInvoiceView::from);
    }

    private BillingAccountView toView(TenantBillingAccount account) {
        BillingPlan plan = account.getPlanId() == null ? null : planRepository.findById(account.getPlanId()).orElse(null);
        return new BillingAccountView(
                account.getStatus(),
                plan == null ? null : plan.getName(),
                plan == null ? null : plan.getMonthlyPrice(),
                plan == null ? null : plan.getCurrency(),
                plan == null ? null : plan.getFeatures(),
                account.getCurrentPeriodEnd(),
                account.getLastBilledAt(),
                account.getCardBrand(),
                account.getCardLast4(),
                account.getCardExpMonth(),
                account.getCardExpYear());
    }

    // ---- Tenant-facing card management (roadmap #22 decision: tenant Admin self-service) ----

    /** Begins an "add a card" flow; returns a disabled response when Stripe isn't configured. */
    public SetupIntentResponse createCardSetupIntent(Long tenantId) {
        if (!stripeConfigured()) {
            return new SetupIntentResponse(false, null);
        }
        try {
            Stripe.apiKey = stripeKey;
            Map<String, Object> params = new HashMap<>();
            params.put("usage", "off_session");
            params.put("payment_method_types", List.of("card"));
            SetupIntent intent = SetupIntent.create(params);
            return new SetupIntentResponse(true, intent.getClientSecret());
        } catch (Exception e) {
            log.warn("Failed to create billing SetupIntent for tenant {}: {}", tenantId, e.getMessage());
            return new SetupIntentResponse(false, null);
        }
    }

    /** Records a confirmed Stripe PaymentMethod as the tenant's one card on file, replacing any prior one. */
    @Transactional
    public void recordPaymentMethod(Long tenantId, String paymentMethodId) {
        if (!stripeConfigured()) {
            throw new IllegalStateException("Card saving isn't available — payments aren't configured.");
        }
        TenantBillingAccount account = accountRepository.findByTenantId(tenantId).orElseGet(() -> newAccountFor(tenantId));
        try {
            Stripe.apiKey = stripeKey;
            detachFromStripe(account.getStripePaymentMethodId());
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            account.setStripePaymentMethodId(pm.getId());
            if (pm.getCard() != null) {
                account.setCardBrand(pm.getCard().getBrand());
                account.setCardLast4(pm.getCard().getLast4());
                account.setCardExpMonth(pm.getCard().getExpMonth() == null ? null : pm.getCard().getExpMonth().intValue());
                account.setCardExpYear(pm.getCard().getExpYear() == null ? null : pm.getCard().getExpYear().intValue());
            }
            accountRepository.save(account);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not save that card: " + e.getMessage(), e);
        }
    }

    private void detachFromStripe(String paymentMethodId) {
        if (paymentMethodId == null) {
            return;
        }
        try {
            PaymentMethod.retrieve(paymentMethodId).detach();
        } catch (Exception e) {
            log.warn("Could not detach billing payment method {} from Stripe: {}", paymentMethodId, e.getMessage());
        }
    }

    // ---- Monthly charge sweep (MonthlyBillingScheduler) ----

    /**
     * Charges every account whose {@code currentPeriodEnd} is due, independently — one tenant's Stripe
     * failure never affects another's, mirroring {@code AbandonedCartService.remindStale()}. Every
     * branch writes a {@link BillingInvoice} row and returns normally; this never throws.
     */
    @Transactional
    public int chargeDueAccounts() {
        List<TenantBillingAccount> due =
                accountRepository.findAllByStatusInAndCurrentPeriodEndLessThanEqual(DUE_STATUSES, new Date());
        for (TenantBillingAccount account : due) {
            chargeOne(account);
        }
        if (!due.isEmpty()) {
            log.info("Monthly billing sweep processed {} due account(s).", due.size());
        }
        return due.size();
    }

    private void chargeOne(TenantBillingAccount account) {
        BillingPlan plan = account.getPlanId() == null ? null : planRepository.findById(account.getPlanId()).orElse(null);
        if (plan == null) {
            return; // defensive; shouldn't occur for an ACTIVE/PAST_DUE account
        }
        if (!stripeConfigured()) {
            recordInvoice(account, plan, "SKIPPED", "Stripe not configured", null);
            return; // leave status/currentPeriodEnd untouched — retries automatically once Stripe is set up
        }
        if (account.getStripePaymentMethodId() == null) {
            recordInvoice(account, plan, "FAILED", "No payment method on file", null);
            account.setStatus("PAST_DUE");
            accountRepository.save(account);
            return;
        }
        try {
            Stripe.apiKey = stripeKey;
            Map<String, Object> params = new HashMap<>();
            params.put("amount", plan.getMonthlyPrice().multiply(BigDecimal.valueOf(100)).longValue());
            params.put("currency", plan.getCurrency().toLowerCase());
            params.put("payment_method", account.getStripePaymentMethodId());
            params.put("confirm", true);
            params.put("off_session", true);
            params.put("description", "Subscription charge: " + plan.getName());
            PaymentIntent intent = PaymentIntent.create(params);
            recordInvoice(account, plan, "SUCCEEDED", null, intent.getId());
            account.setStatus("ACTIVE");
            account.setCurrentPeriodEnd(oneMonthFrom(account.getCurrentPeriodEnd()));
            account.setLastBilledAt(new Date());
            accountRepository.save(account);
        } catch (StripeException e) {
            recordInvoice(account, plan, "FAILED", e.getMessage(), null);
            account.setStatus("PAST_DUE");
            accountRepository.save(account);
        }
    }

    private void recordInvoice(TenantBillingAccount account, BillingPlan plan, String status, String failureReason, String paymentIntentId) {
        BillingInvoice invoice = new BillingInvoice();
        invoice.setTenantId(account.getTenantId());
        invoice.setBillingAccountId(account.getId());
        invoice.setPlanNameSnapshot(plan.getName());
        invoice.setAmount(plan.getMonthlyPrice());
        invoice.setCurrency(plan.getCurrency());
        invoice.setStatus(status);
        invoice.setFailureReason(failureReason);
        invoice.setStripePaymentIntentId(paymentIntentId);
        invoiceRepository.save(invoice);
    }

    private static Date oneMonthFrom(Date date) {
        Instant base = date == null ? Instant.now() : date.toInstant();
        return Date.from(base.atZone(ZoneId.systemDefault()).plusMonths(1).toInstant());
    }
}
