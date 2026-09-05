package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Admin create/update payload for a promotion. Set percentOff or amountOff. Present id to update. */
public record PromotionRequest(
        Long id,
        @NotBlank(message = "Name is required") String name,
        String description,
        Integer percentOff,
        BigDecimal amountOff,
        BigDecimal minSpend,
        boolean active,
        LocalDate startsAt,
        LocalDate endsAt) {
}
