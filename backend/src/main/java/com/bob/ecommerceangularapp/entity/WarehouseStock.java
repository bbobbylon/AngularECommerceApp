package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Units of one SKU held at one {@link Warehouse} (roadmap #20). SKU-keyed like the inventory view
 * from roadmap #15: a product's own SKU for single-SKU products, the variant SKU for products sold
 * per-variant. Creating a shipment decrements these rows (clamped at zero); the product/variant
 * {@code unitsInStock} total is untouched by fulfillment — checkout already decrements it at
 * order time.
 */
@Entity
@Table(name = "warehouse_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_stock", columnNames = {"warehouse_id", "sku"}))
@Getter
@Setter
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
