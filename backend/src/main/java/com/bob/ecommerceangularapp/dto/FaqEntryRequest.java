package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin create/update payload for a FAQ entry. Present id to update. */
public record FaqEntryRequest(
        Long id,
        @NotBlank(message = "Question is required") String question,
        @NotBlank(message = "Answer is required") String answer,
        int sortOrder,
        boolean active) {
}
