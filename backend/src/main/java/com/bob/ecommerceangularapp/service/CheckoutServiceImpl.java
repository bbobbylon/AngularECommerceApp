package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dto.PaymentInfo;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CustomerRepository customerRepository;

    public CheckoutServiceImpl(CustomerRepository customerRepository,
                               @Value("${stripe.key.secret}") String secretKey) {
        this.customerRepository = customerRepository;
        // Stripe is keyed globally via a static field.
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional
    public PurchaseResponse placeOrder(Purchase purchase) {

        Order order = purchase.getOrder();

        String orderTrackingNumber = generateOrderTrackingNumber();
        order.setOrderTrackingNumber(orderTrackingNumber);

        // populate order with its items (maintains the bidirectional link)
        Set<OrderItem> orderItems = purchase.getOrderItems();
        orderItems.forEach(order::add);

        // populate order with its addresses
        order.setShippingAddress(purchase.getShippingAddress());
        order.setBillingAddress(purchase.getBillingAddress());

        // populate customer with the order, reusing an existing customer if the email matches
        Customer customer = purchase.getCustomer();
        Customer existingCustomer = customerRepository.findByEmail(customer.getEmail());
        if (existingCustomer != null) {
            customer = existingCustomer;
        }
        customer.add(order);

        // cascade persists the order, items and addresses
        customerRepository.save(customer);

        return new PurchaseResponse(orderTrackingNumber);
    }

    private String generateOrderTrackingNumber() {
        return UUID.randomUUID().toString();
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException {
        List<String> paymentMethodTypes = new ArrayList<>();
        paymentMethodTypes.add("card");

        Map<String, Object> params = new HashMap<>();
        params.put("amount", paymentInfo.getAmount());
        params.put("currency", paymentInfo.getCurrency());
        params.put("payment_method_types", paymentMethodTypes);
        params.put("description", "Luv2Shop purchase");
        if (paymentInfo.getReceiptEmail() != null) {
            params.put("receipt_email", paymentInfo.getReceiptEmail());
        }

        return PaymentIntent.create(params);
    }
}
