package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Loyalty ledger — served via the custom loyalty controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findTop20ByCustomerEmailIgnoreCaseOrderByDateCreatedDesc(String email);
}
