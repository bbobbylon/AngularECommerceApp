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
 * A "notify me when back in stock" subscription. {@code variantSku} is null for a product-level
 * subscription, or the variant SKU for a variant-level one. {@code notified} flips true once the
 * restock email has been sent, so each subscriber is mailed at most once per subscription.
 */
@Entity
@Table(name = "stock_notification",
        indexes = @Index(name = "idx_stock_notification_product", columnList = "product_id"))
@Getter
@Setter
public class StockNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "variant_sku")
    private String variantSku;

    @Column(name = "email")
    private String email;

    @Column(name = "notified")
    private boolean notified;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
