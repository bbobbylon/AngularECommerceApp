package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Loyalty ledger — served via the custom loyalty controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could leak a same-email customer's rewards history across two different storefronts.
    List<LoyaltyTransaction> findTop20ByCustomerEmailIgnoreCaseAndTenantIdOrderByDateCreatedDesc(String email, Long tenantId);
}
