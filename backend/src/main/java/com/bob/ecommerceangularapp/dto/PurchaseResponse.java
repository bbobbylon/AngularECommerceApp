package com.bob.ecommerceangularapp.dto;

import lombok.Getter;

@Getter
public class PurchaseResponse {

    private final String orderTrackingNumber;

    public PurchaseResponse(String orderTrackingNumber) {
        this.orderTrackingNumber = orderTrackingNumber;
    }
}
