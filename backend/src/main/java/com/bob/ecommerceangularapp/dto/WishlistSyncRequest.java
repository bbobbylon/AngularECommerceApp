package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Merge a device's local wishlist into the account's saved wishlist. */
public record WishlistSyncRequest(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
        List<Long> productIds) {
}
