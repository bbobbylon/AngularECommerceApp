package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * A fulfillment location (roadmap #20). Warehouses hold a per-SKU stock distribution
 * ({@link WarehouseStock}) that shipments draw down from; the product/variant
 * {@code unitsInStock} remains the authoritative sellable total the storefront and checkout use —
 * warehouse rows describe where that stock physically sits for fulfillment.
 * {@code priority} orders warehouses when suggesting where to ship from (lower = preferred).
 */
@Entity
@Table(name = "warehouse", uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_code", columnNames = "code"))
@Getter
@Setter
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Short unique handle shown on shipments (e.g. "ATL-EAST"). */
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    /** Allocation preference — lower ships first when coverage ties. */
    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
