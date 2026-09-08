package com.bob.ecommerceangularapp.dto;

import lombok.Getter;

/** {@code CheckoutService.placeOrder}'s result — just enough for the order-confirmation page to look the order up. */
@Getter
public class PurchaseResponse {

    private final String orderTrackingNumber;

    public PurchaseResponse(String orderTrackingNumber) {
        this.orderTrackingNumber = orderTrackingNumber;
    }
}
