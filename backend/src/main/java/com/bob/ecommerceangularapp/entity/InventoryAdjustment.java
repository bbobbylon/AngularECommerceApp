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
 * Audit trail row for a single SKU's stock level changing — either a manual admin edit or a bulk CSV
 * import (see {@link com.bob.ecommerceangularapp.service.InventoryService}). {@code sku} is either a
 * {@link Product#getSku()} (products with no variants) or a {@link ProductVariant#getSku()} (products
 * sold per-variant); {@code productName} is a display-time snapshot so history still reads sensibly if
 * the product is later renamed or deleted.
 */
@Entity
@Table(name = "inventory_adjustment", indexes = @Index(name = "idx_inventory_adjustment_sku", columnList = "sku"))
@Getter
@Setter
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sku")
    private String sku;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "previous_quantity")
    private int previousQuantity;

    @Column(name = "new_quantity")
    private int newQuantity;

    @Column(name = "delta")
    private int delta;

    /** "MANUAL" (single admin edit) or "CSV_IMPORT" (bulk upload). */
    @Column(name = "source")
    private String source;

    @Column(name = "note")
    private String note;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
