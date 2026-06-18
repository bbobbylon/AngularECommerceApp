package com.bob.ecommerceangularapp.dto;

import java.util.Date;

/** One row in a customer's points history. */
public record LoyaltyTransactionView(String type, int points, String description, Date dateCreated) {
}
