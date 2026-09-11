package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.BillingPlanRepository;
import com.bob.ecommerceangularapp.dto.PlatformBillingPlanRequest;
import com.bob.ecommerceangularapp.entity.BillingPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Platform-level billing-plan catalog (roadmap #22) — create/list/edit/deactivate the plans tenants
 * can be put on. Gated on the {@code SuperAdmin} role in {@code SecurityConfig}, same as
 * {@link PlatformTenantService}, which this mirrors closely: a plan is platform-level data that sits
 * above the tenant boundary, so this service never reads {@code TenantContext}.
 */
@Service
public class BillingPlanService {

    private final BillingPlanRepository billingPlanRepository;

    public BillingPlanService(BillingPlanRepository billingPlanRepository) {
        this.billingPlanRepository = billingPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<BillingPlan> list() {
        return billingPlanRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional
    public BillingPlan save(PlatformBillingPlanRequest request) {
        String name = request.name().trim();
        boolean nameTaken = request.id() == null
                ? billingPlanRepository.existsByName(name)
                : billingPlanRepository.existsByNameAndIdNot(name, request.id());
        if (nameTaken) {
            throw new IllegalArgumentException("A billing plan named \"" + name + "\" already exists.");
        }

        BillingPlan plan = request.id() == null
                ? new BillingPlan()
                : billingPlanRepository.findById(request.id())
                        .orElseThrow(() -> new IllegalArgumentException("Billing plan not found: " + request.id()));
        plan.setName(name);
        plan.setMonthlyPrice(request.monthlyPrice());
        plan.setCurrency(blankToNull(request.currency()) == null ? "USD" : request.currency().trim().toUpperCase());
        plan.setFeatures(request.features());
        plan.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        plan.setActive(request.active() == null || request.active());
        return billingPlanRepository.save(plan);
    }

    @Transactional
    public void deactivate(Long id) {
        BillingPlan plan = billingPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Billing plan not found: " + id));
        plan.setActive(false);
        billingPlanRepository.save(plan);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
