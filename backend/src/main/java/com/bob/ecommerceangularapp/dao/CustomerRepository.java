package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Not exposed over REST — orders are created through the CheckoutController only. */
@RepositoryRestResource(exported = false)
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Renamed from findByEmail(String) (roadmap #21): email is not unique across tenants, so every
    // lookup must be scoped or two tenants' same-email customers could merge into one record.
    Customer findByEmailAndTenantId(String email, Long tenantId);

    // Global by design: the code itself is a random, DB-unique string, so collision-checking a
    // freshly-generated one doesn't need to be tenant-scoped.
    boolean existsByReferralCode(String referralCode);

    // Scoped to tenant (roadmap #21, Milestone D): a referral code is unique across all tenants, but a
    // referee on tenant B's storefront must never be able to reward tenant A's referrer with points —
    // that's a real cross-tenant financial leak, not just a data-visibility one.
    Customer findByReferralCodeAndTenantId(String referralCode, Long tenantId);

    Customer findByUnsubscribeToken(String unsubscribeToken);

    List<Customer> findByNewsletterSubscribedTrue();

    long countByNewsletterSubscribedTrue();

    // ----- admin back office, tenant-scoped (roadmap #21, Milestone B) -----
    long countByTenantId(Long tenantId);

    long countByTenantIdAndNewsletterSubscribedTrue(Long tenantId);
}
