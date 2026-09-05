package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * One fulfillment of an order from a {@link Warehouse} (roadmap #20). Lifecycle:
 * {@code PENDING} (picked/packed, warehouse stock already drawn down) → {@code SHIPPED} (carrier +
 * tracking live, order status synced to "Shipped") → {@code DELIVERED} (order synced to
 * "Delivered"). Keyed to the order by id + denormalized tracking number, like
 * {@link ReturnRequest} — the customer-facing lookup matches on order tracking number + email.
 */
@Entity
@Table(name = "shipment", indexes = @Index(name = "idx_shipment_order", columnList = "order_id"))
@Getter
@Setter
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_tracking_number")
    private String orderTrackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /** e.g. "UPS", "FedEx", "USPS" — free text, the admin types it when shipping. */
    @Column(name = "carrier", length = 64)
    private String carrier;

    /** The carrier's tracking number (distinct from the order's own tracking number). */
    @Column(name = "tracking_number", length = 128)
    private String trackingNumber;

    /** PENDING | SHIPPED | DELIVERED */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "shipped_at")
    private Date shippedAt;

    @Column(name = "delivered_at")
    private Date deliveredAt;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
