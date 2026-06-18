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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A snapshot of a cart that reached checkout but wasn't completed, captured by email so a recovery
 * email can be sent later. One live (unrecovered) row per email — re-captured as the cart changes.
 * {@code recovered} flips when the customer places an order; {@code reminded} once the email is sent.
 */
@Entity
@Table(name = "abandoned_cart",
        indexes = @Index(name = "idx_abandoned_cart_email", columnList = "email"))
@Getter
@Setter
public class AbandonedCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "item_count")
    private int itemCount;

    @Column(name = "total")
    private BigDecimal total;

    /** Short human summary of the cart contents (for the email). */
    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "recovered")
    private boolean recovered;

    @Column(name = "reminded")
    private boolean reminded;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;

    @Column(name = "last_updated")
    @UpdateTimestamp
    private Date lastUpdated;
}
