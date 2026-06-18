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
 * A completed referral: {@code referrerCode} (the existing customer's code) brought in
 * {@code refereeEmail} (a new customer) on their first order. Both sides are rewarded with loyalty
 * points ({@code referrerPoints} / {@code refereePoints}). One referral per referee.
 */
@Entity
@Table(name = "referral",
        indexes = {
                @Index(name = "idx_referral_referrer", columnList = "referrer_code"),
                @Index(name = "idx_referral_referee", columnList = "referee_email")
        })
@Getter
@Setter
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "referrer_code")
    private String referrerCode;

    @Column(name = "referee_email")
    private String refereeEmail;

    /** PENDING | COMPLETED (we record at first purchase, so typically COMPLETED). */
    @Column(name = "status")
    private String status;

    @Column(name = "referrer_points")
    private int referrerPoints;

    @Column(name = "referee_points")
    private int refereePoints;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
