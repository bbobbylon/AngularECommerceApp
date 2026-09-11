package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Create payload for a product category (admin). */
public record CategoryRequest(@NotBlank(message = "Category name is required") String name) {
}
