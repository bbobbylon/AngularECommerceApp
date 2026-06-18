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
 * A card a customer saved for reuse. Only Stripe references + display metadata are stored — never raw
 * card data (PCI: the card lives in Stripe, we keep the PaymentMethod id + brand/last4/expiry). Keyed
 * by email.
 */
@Entity
@Table(name = "saved_payment_method",
        indexes = @Index(name = "idx_saved_pm_email", columnList = "email"))
@Getter
@Setter
public class SavedPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "stripe_payment_method_id")
    private String stripePaymentMethodId;

    @Column(name = "brand")
    private String brand;

    @Column(name = "last4")
    private String last4;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    @Column(name = "default_method")
    private boolean defaultMethod;
}
