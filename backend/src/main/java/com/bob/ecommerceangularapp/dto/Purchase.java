package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class Purchase {

    private Customer customer;
    private Address shippingAddress;
    private Address billingAddress;
    private Order order;
    private Set<OrderItem> orderItems;

    /** Checkout opt-in: create the account on the weekly-deals list (defaults to opted-in). */
    private boolean subscribeToNewsletter = true;

    /** Applied coupon (optional). The server re-validates and records the discount on the order. */
    private String couponCode;
    private java.math.BigDecimal subtotal;

    /** Chosen shipping method code; the server recomputes shipping + tax authoritatively from it. */
    private String shippingMethodCode;

    /** Stripe PaymentIntent id when paid by card — recorded on the order so a return can refund it. */
    private String paymentIntentId;

    /** Gift card code to redeem as store credit against the order total (optional). */
    private String giftCardCode;

    /** Loyalty points the customer chose to redeem as store credit (server-validated against balance). */
    private int pointsToRedeem;
}
