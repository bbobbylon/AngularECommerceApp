package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Referral ledger — served via the custom referral controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    boolean existsByRefereeEmailIgnoreCase(String refereeEmail);

    List<Referral> findByReferrerCode(String referrerCode);
}
