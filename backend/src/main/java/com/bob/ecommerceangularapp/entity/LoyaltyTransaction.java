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
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * An entry in a customer's loyalty-points ledger. {@code points} is always positive; {@code type}
 * ({@code EARN}/{@code REDEEM}) gives the direction. Keyed by customer email (the app's customer
 * identity); linked to an order where applicable.
 */
@Entity
@Table(name = "loyalty_transaction",
        indexes = @Index(name = "idx_loyalty_tx_email", columnList = "customer_email"))
@Getter
@Setter
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_email")
    private String customerEmail;

    /** EARN | REDEEM */
    @Column(name = "type")
    private String type;

    @Column(name = "points")
    private int points;

    @Column(name = "description")
    private String description;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
