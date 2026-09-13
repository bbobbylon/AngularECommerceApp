package com.bob.ecommerceangularapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Raises a data-subject request (roadmap #24). {@code requestType} is
 * {@code EXPORT} or {@code ERASURE}; anything else is rejected by the service.
 *
 * <p>Supplying an email here proves nothing — the request stays inert until the link mailed to
 * that address is clicked. See {@link com.bob.ecommerceangularapp.entity.DataRequest}.
 */
public record DataRequestSubmission(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
        @NotBlank(message = "A request type is required") String requestType) {
}
