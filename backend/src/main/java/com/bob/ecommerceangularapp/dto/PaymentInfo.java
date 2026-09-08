package com.bob.ecommerceangularapp.dto;

import lombok.Getter;
import lombok.Setter;

/** Inputs for {@code CheckoutService.createPaymentIntent} — the amount Stripe should charge (in cents). */
@Getter
@Setter
public class PaymentInfo {

    private int amount;
    private String currency;
    private String receiptEmail;
}
