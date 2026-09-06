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

    Customer findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    Customer findByUnsubscribeToken(String unsubscribeToken);

    List<Customer> findByNewsletterSubscribedTrue();

    long countByNewsletterSubscribedTrue();
}
