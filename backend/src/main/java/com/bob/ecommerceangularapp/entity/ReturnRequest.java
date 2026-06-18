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
 * A customer's request to return an order (RMA). Lifecycle: {@code REQUESTED} → {@code APPROVED} /
 * {@code DENIED}; an approved return becomes {@code REFUNDED} once a Stripe refund succeeds (or stays
 * {@code APPROVED} for a manual/offline refund when Stripe isn't configured). Keyed to an order by id
 * + tracking number (denormalized for display); {@code customerEmail} must match the order's customer.
 */
@Entity
@Table(name = "return_request",
        indexes = @Index(name = "idx_return_request_order", columnList = "order_id"))
@Getter
@Setter
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_tracking_number")
    private String orderTrackingNumber;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "reason", length = 2000)
    private String reason;

    /** REQUESTED | APPROVED | DENIED | REFUNDED */
    @Column(name = "status")
    private String status;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "admin_note", length = 2000)
    private String adminNote;

    /** Stripe refund id once a real refund is issued (null for manual/offline refunds). */
    @Column(name = "stripe_refund_id")
    private String stripeRefundId;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;

    @Column(name = "date_updated")
    @UpdateTimestamp
    private Date dateUpdated;
}
