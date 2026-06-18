package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A reusable address saved to a customer's account (keyed by email — the app's customer identity).
 * One address per customer may be the {@code defaultAddress}, pre-selected at checkout.
 */
@Entity
@Table(name = "saved_address",
        indexes = @Index(name = "idx_saved_address_email", columnList = "email"))
@Getter
@Setter
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;

    /** Friendly label, e.g. "Home", "Work". */
    @Column(name = "label")
    private String label;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "default_address")
    private boolean defaultAddress;
}
