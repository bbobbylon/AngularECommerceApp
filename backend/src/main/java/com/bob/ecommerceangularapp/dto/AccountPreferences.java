package com.bob.ecommerceangularapp.dto;

/** Account/email-preferences view returned by the account settings portal. */
public record AccountPreferences(String firstName, String lastName, String email, boolean newsletterSubscribed) {
}
