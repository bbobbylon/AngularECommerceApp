package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Cart snapshot captured at checkout so a recovery email can be sent if the order isn't completed. */
public record AbandonedCartRequest(
        @NotBlank(message = "Email is required") @Email(message = "A valid email is required") String email,
        int itemCount,
        BigDecimal total,
        String summary) {
}
