package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dto.PaymentInfo;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

/**
 * The checkout pipeline's contract. {@link CheckoutServiceImpl} is the sole implementation and the
 * single place nearly every commerce feature (coupons, promotions, gift cards, loyalty, referrals,
 * tax/shipping, abandoned-cart recovery, Stripe) composes together — see its class doc for the order
 * discounts/adjustments are applied in. Called by {@code CheckoutController}.
 */
public interface CheckoutService {

    /** Validates, prices, and persists an order from the cart the customer just submitted. */
    PurchaseResponse placeOrder(Purchase purchase);

    /** Creates the Stripe PaymentIntent the frontend's card element confirms against. */
    PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException;
}
