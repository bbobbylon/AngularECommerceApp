package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against a real cross-tenant leak found during roadmap #21's tenant-scoping audit:
 * {@code findByCustomerEmailOrderByDateCreatedDesc} (no tenant predicate) was exported as a raw
 * Spring Data REST search resource, so any caller who knew (or guessed) a customer's email could read
 * their full order history regardless of which tenant it belonged to — see {@code AccountController}
 * and {@code OrderRepository}, which replaced it with the tenant-scoped method tested here.
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByCustomerEmailAndTenantId_neverCrossesTenants() {
        Order tenantAOrder = order(1L, "shopper@example.com", "TRACK-A");
        Order tenantBOrder = order(2L, "shopper@example.com", "TRACK-B");
        orderRepository.save(tenantAOrder);
        orderRepository.save(tenantBOrder);

        Page<Order> forA = orderRepository.findByCustomerEmailAndTenantIdOrderByDateCreatedDesc(
                "shopper@example.com", 1L, PageRequest.of(0, 10));
        Page<Order> forB = orderRepository.findByCustomerEmailAndTenantIdOrderByDateCreatedDesc(
                "shopper@example.com", 2L, PageRequest.of(0, 10));

        assertThat(forA.getContent()).extracting(Order::getOrderTrackingNumber).containsExactly("TRACK-A");
        assertThat(forB.getContent()).extracting(Order::getOrderTrackingNumber).containsExactly("TRACK-B");
    }

    @Test
    void findByCustomerEmailAndTenantId_emptyForUnknownTenant() {
        orderRepository.save(order(1L, "known@example.com", "TRACK-X"));

        Page<Order> forOtherTenant = orderRepository.findByCustomerEmailAndTenantIdOrderByDateCreatedDesc(
                "known@example.com", 99L, PageRequest.of(0, 10));

        assertThat(forOtherTenant.getContent()).isEmpty();
    }

    private Order order(Long tenantId, String email, String trackingNumber) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setEmail(email);
        customer.setFirstName("Test");
        customerRepository.save(customer);

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setCustomer(customer);
        order.setOrderTrackingNumber(trackingNumber);
        order.setTotalQuantity(1);
        order.setTotalPrice(new BigDecimal("10.00"));
        order.setStatus("Received");
        return order;
    }
}
