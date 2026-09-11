package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Records a Stripe PaymentMethod the customer just set up, so it appears as a saved card. */
public record RecordPaymentMethodRequest(
        @NotBlank(message = "paymentMethodId is required") String paymentMethodId) {
}
