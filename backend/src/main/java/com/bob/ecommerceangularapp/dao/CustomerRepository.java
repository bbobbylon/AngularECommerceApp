package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Not exposed over REST — orders are created through the CheckoutController only. */
@RepositoryRestResource(exported = false)
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Customer findByEmail(String email);

    Customer findByUnsubscribeToken(String unsubscribeToken);

    List<Customer> findByNewsletterSubscribedTrue();
}
