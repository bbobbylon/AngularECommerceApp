package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Create/update payload for a saved address ({@code id} null when creating). */
public record SavedAddressRequest(
        Long id,
        String label,
        String recipientName,
        @NotBlank(message = "Street is required") String street,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "State is required") String state,
        @NotBlank(message = "Country is required") String country,
        @NotBlank(message = "Zip code is required") String zipCode,
        boolean defaultAddress) {
}
