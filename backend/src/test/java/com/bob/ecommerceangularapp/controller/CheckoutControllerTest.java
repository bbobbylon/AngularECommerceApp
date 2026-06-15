package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: drives the checkout controller against the full Spring context and the
 * in-memory H2 database, verifying the cascade-persist of customer -> order -> items/addresses.
 * Transactional so each run rolls back.
 */
@SpringBootTest
@Transactional
class CheckoutControllerTest {

    @Autowired
    private CheckoutController checkoutController;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void placeOrder_persistsTheOrderGraphAndReturnsTrackingNumber() {
        long ordersBefore = orderRepository.count();

        PurchaseResponse response = checkoutController.placeOrder(buildPurchase());

        assertThat(response.getOrderTrackingNumber()).isNotBlank();
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);

        Customer customer = customerRepository.findByEmail("integration@test.com");
        assertThat(customer).isNotNull();
        assertThat(customer.getOrders()).hasSize(1);

        Order saved = customer.getOrders().iterator().next();
        assertThat(saved.getOrderTrackingNumber()).isEqualTo(response.getOrderTrackingNumber());
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getShippingAddress()).isNotNull();
        assertThat(saved.getBillingAddress()).isNotNull();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("21.98");
    }

    private Purchase buildPurchase() {
        Purchase purchase = new Purchase();

        Customer customer = new Customer();
        customer.setFirstName("Inte");
        customer.setLastName("Gration");
        customer.setEmail("integration@test.com");
        purchase.setCustomer(customer);

        purchase.setShippingAddress(buildAddress("123 Test St"));
        purchase.setBillingAddress(buildAddress("123 Test St"));

        Order order = new Order();
        order.setTotalQuantity(2);
        order.setTotalPrice(new BigDecimal("21.98"));
        order.setStatus("Received");
        purchase.setOrder(order);

        OrderItem item = new OrderItem();
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.99"));
        item.setProductId(1L);
        item.setImageUrl("https://example.test/p.png");
        purchase.setOrderItems(Set.of(item));

        return purchase;
    }

    private Address buildAddress(String street) {
        Address address = new Address();
        address.setStreet(street);
        address.setCity("Testville");
        address.setState("California");
        address.setCountry("United States");
        address.setZipCode("90210");
        return address;
    }
}
