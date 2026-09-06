package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the real cross-tenant leak found while implementing roadmap #21 (Milestone A):
 * {@code CheckoutServiceImpl} used to reuse an existing customer purely by email, which — since email
 * is not unique — could silently merge two different tenants' same-email customers into one record.
 */
@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByEmailAndTenantId_neverMergesSameEmailAcrossTenants() {
        Customer tenantACustomer = new Customer();
        tenantACustomer.setTenantId(1L);
        tenantACustomer.setEmail("shopper@example.com");
        tenantACustomer.setFirstName("Alice");
        customerRepository.save(tenantACustomer);

        Customer tenantBCustomer = new Customer();
        tenantBCustomer.setTenantId(2L);
        tenantBCustomer.setEmail("shopper@example.com");
        tenantBCustomer.setFirstName("Bob");
        customerRepository.save(tenantBCustomer);

        Customer foundForA = customerRepository.findByEmailAndTenantId("shopper@example.com", 1L);
        Customer foundForB = customerRepository.findByEmailAndTenantId("shopper@example.com", 2L);

        assertThat(foundForA.getFirstName()).isEqualTo("Alice");
        assertThat(foundForB.getFirstName()).isEqualTo("Bob");
        assertThat(foundForA.getId()).isNotEqualTo(foundForB.getId());
    }

    @Test
    void findByEmailAndTenantId_returnsNullForUnknownTenant() {
        Customer customer = new Customer();
        customer.setTenantId(1L);
        customer.setEmail("known@example.com");
        customerRepository.save(customer);

        assertThat(customerRepository.findByEmailAndTenantId("known@example.com", 99L)).isNull();
    }
}
