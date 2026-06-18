package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * A customer's request to open a return. The {@code email} must match the order's customer (so a
 * tracking number alone can't be used to file someone else's return).
 */
public record CreateReturnRequest(
        @NotBlank(message = "Order tracking number is required") String orderTrackingNumber,
        @NotBlank(message = "Email is required") @Email(message = "A valid email is required") String email,
        @NotBlank(message = "Please tell us why you're returning the item(s)") String reason) {
}
