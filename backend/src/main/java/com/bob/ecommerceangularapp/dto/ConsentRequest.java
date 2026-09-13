package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A visitor's cookie/storage choice, posted by the consent banner (roadmap #24).
 *
 * <p>{@code necessary} is absent on purpose — it is not the visitor's to decline, so accepting it
 * as input would imply a choice that does not exist. The service always records it as true.
 */
public record ConsentRequest(
        @NotBlank(message = "A visitor id is required") @Size(max = 64) String visitorId,
        String email,
        boolean functional,
        boolean analytics,
        boolean marketing,
        String source) {
}
