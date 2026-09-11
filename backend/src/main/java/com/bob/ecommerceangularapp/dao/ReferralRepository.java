package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Referral ledger — served via the custom referral controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // check could wrongly block (or allow) a referral based on another tenant's referee history.
    boolean existsByRefereeEmailIgnoreCaseAndTenantId(String refereeEmail, Long tenantId);

    List<Referral> findByReferrerCodeAndTenantId(String referrerCode, Long tenantId);
}
