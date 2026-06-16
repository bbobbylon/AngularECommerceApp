package com.bob.ecommerceangularapp.dto;

/** Update payload from the account settings portal. {@code newsletterSubscribed} may be null (left unchanged). */
public record AccountUpdateRequest(String email, String firstName, String lastName, Boolean newsletterSubscribed) {
}
