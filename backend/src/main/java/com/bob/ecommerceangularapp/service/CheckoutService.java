package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dto.PaymentInfo;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

public interface CheckoutService {

    PurchaseResponse placeOrder(Purchase purchase);

    PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException;
}
