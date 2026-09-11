package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.TenantBillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Per-tenant billing accounts (roadmap #22) — served via AdminBilling/Platform controllers, not SDR. */
@RepositoryRestResource(exported = false)
public interface TenantBillingAccountRepository extends JpaRepository<TenantBillingAccount, Long> {

    Optional<TenantBillingAccount> findByTenantId(Long tenantId);

    // Unscoped by design: read only by the monthly billing sweep, a background job with no per-tenant
    // request context that charges every due account across every tenant (see MonthlyBillingScheduler),
    // the same idiom AbandonedCartRepository.findByRecoveredFalseAndRemindedFalseAndLastUpdatedBefore uses.
    List<TenantBillingAccount> findAllByStatusInAndCurrentPeriodEndLessThanEqual(List<String> statuses, Date cutoff);
}
