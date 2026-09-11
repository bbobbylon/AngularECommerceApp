package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.AbandonedCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Abandoned-cart snapshots — served via the custom controller + scheduler, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface AbandonedCartRepository extends JpaRepository<AbandonedCart, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could merge/recover a same-email customer's cart snapshot across two different storefronts.
    Optional<AbandonedCart> findFirstByEmailIgnoreCaseAndRecoveredFalseAndTenantIdOrderByIdDesc(String email, Long tenantId);

    List<AbandonedCart> findByEmailIgnoreCaseAndRecoveredFalseAndTenantId(String email, Long tenantId);

    // Unscoped by design: read only by the reminder scheduler, a background job with no per-tenant
    // request context that emails every idle cart across every tenant (see AbandonedCartScheduler).
    List<AbandonedCart> findByRecoveredFalseAndRemindedFalseAndLastUpdatedBefore(Date cutoff);
}
