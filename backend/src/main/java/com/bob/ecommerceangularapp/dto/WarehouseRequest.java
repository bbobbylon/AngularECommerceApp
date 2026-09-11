package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin upsert body for a warehouse — null id creates, non-null updates (same pattern as tax/shipping). */
public record WarehouseRequest(
        Long id,
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String city,
        @Size(max = 255) String state,
        @Size(max = 255) String country,
        Integer priority,
        Boolean active) {
}
