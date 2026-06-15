package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Not exposed over REST — orders are created through the CheckoutController only. */
@RepositoryRestResource(exported = false)
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Customer findByEmail(String email);
}
