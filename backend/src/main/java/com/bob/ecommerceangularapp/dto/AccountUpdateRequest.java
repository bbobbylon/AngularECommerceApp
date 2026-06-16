package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Update payload from the account settings portal. {@code newsletterSubscribed} may be null (left unchanged). */
public record AccountUpdateRequest(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
        String firstName,
        String lastName,
        Boolean newsletterSubscribed) {
}
