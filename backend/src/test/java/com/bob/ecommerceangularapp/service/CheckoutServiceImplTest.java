package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (no Spring context / no DB) for the order-building logic.
 */
class CheckoutServiceImplTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final TaxShippingService taxShippingService = mock(TaxShippingService.class);
    private final ProductVariantService productVariantService = mock(ProductVariantService.class);
    private final CheckoutServiceImpl service =
            new CheckoutServiceImpl(customerRepository, emailService, taxShippingService, productVariantService, "");

    @Test
    void placeOrder_generatesTrackingNumberAndLinksEntities() {
        Purchase purchase = buildPurchase();
        when(customerRepository.findByEmail(anyString())).thenReturn(null);

        PurchaseResponse response = service.placeOrder(purchase);

        assertThat(response.getOrderTrackingNumber()).isNotBlank();

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();

        assertThat(saved.getOrders()).hasSize(1);
        Order order = saved.getOrders().iterator().next();
        assertThat(order.getCustomer()).isSameAs(saved);
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().iterator().next().getOrder()).isSameAs(order);
        assertThat(order.getShippingAddress()).isNotNull();
        assertThat(order.getBillingAddress()).isNotNull();
        assertThat(order.getOrderTrackingNumber()).isEqualTo(response.getOrderTrackingNumber());
    }

    @Test
    void placeOrder_reusesExistingCustomerByEmail() {
        Purchase purchase = buildPurchase();
        Customer existing = new Customer();
        existing.setEmail("a@b.com");
        when(customerRepository.findByEmail("a@b.com")).thenReturn(existing);

        service.placeOrder(purchase);

        verify(customerRepository).save(existing);
        assertThat(existing.getOrders()).hasSize(1);
    }

    private Purchase buildPurchase() {
        Purchase purchase = new Purchase();

        Customer customer = new Customer();
        customer.setEmail("a@b.com");
        customer.setFirstName("A");
        customer.setLastName("B");
        purchase.setCustomer(customer);

        purchase.setShippingAddress(new Address());
        purchase.setBillingAddress(new Address());

        Order order = new Order();
        order.setTotalPrice(new BigDecimal("10.00"));
        order.setTotalQuantity(1);
        purchase.setOrder(order);

        OrderItem item = new OrderItem();
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setProductId(1L);
        purchase.setOrderItems(Set.of(item));

        return purchase;
    }
}
