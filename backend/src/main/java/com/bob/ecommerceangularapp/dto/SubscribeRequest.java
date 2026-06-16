package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Payload for the newsletter signup box. */
public record SubscribeRequest(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
        String name) {
}
