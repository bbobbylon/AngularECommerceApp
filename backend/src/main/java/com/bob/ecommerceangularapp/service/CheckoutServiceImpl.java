package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dto.CouponResponse;
import com.bob.ecommerceangularapp.dto.PaymentInfo;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.email.EmailService;
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
    private final EmailService emailService;
    private final CouponService couponService;
    private final ProductVariantService productVariantService;

    public CheckoutServiceImpl(CustomerRepository customerRepository,
                               EmailService emailService,
                               CouponService couponService,
                               ProductVariantService productVariantService,
                               @Value("${stripe.key.secret}") String secretKey) {
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.couponService = couponService;
        this.productVariantService = productVariantService;
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

        // Draw down SKU-level inventory for any lines bought by variant (no-op for single-SKU items).
        productVariantService.decrementForOrderItems(orderItems);

        // populate order with its addresses
        order.setShippingAddress(purchase.getShippingAddress());
        order.setBillingAddress(purchase.getBillingAddress());

        // Re-validate any coupon server-side and recompute the total authoritatively from the subtotal.
        if (purchase.getCouponCode() != null && !purchase.getCouponCode().isBlank() && purchase.getSubtotal() != null) {
            CouponResponse coupon = couponService.validate(purchase.getCouponCode(), purchase.getSubtotal());
            if (coupon.valid()) {
                order.setCouponCode(coupon.code());
                order.setDiscountAmount(coupon.discount());
                order.setTotalPrice(purchase.getSubtotal().subtract(coupon.discount()));
            }
        }

        // populate customer with the order, reusing an existing customer if the email matches
        Customer customer = purchase.getCustomer();
        Customer existingCustomer = customerRepository.findByEmail(customer.getEmail());
        if (existingCustomer != null) {
            customer = existingCustomer;
        }

        // Apply the checkout newsletter opt-in and track whether this is a fresh subscription
        // (newly created account, or a previously-unsubscribed customer re-opting-in).
        boolean wasSubscribed = customer.isNewsletterSubscribed() && existingCustomer != null;
        customer.setNewsletterSubscribed(purchase.isSubscribeToNewsletter());
        // Every customer gets a token (even opt-outs) so token-presence means "processed" — this lets
        // the startup backfill safely subscribe only pre-existing rows without re-subscribing opt-outs.
        customer.ensureUnsubscribeToken();

        customer.add(order);

        // cascade persists the order, items and addresses
        customerRepository.save(customer);

        // Email is gated inside EmailService — these are safe no-ops when SMTP isn't configured.
        emailService.sendOrderConfirmation(customer.getEmail(), customer.getFirstName(),
                orderTrackingNumber, order.getTotalPrice());
        if (customer.isNewsletterSubscribed() && !wasSubscribed) {
            emailService.sendWelcome(customer.getEmail(), customer.getFirstName());
        }

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
