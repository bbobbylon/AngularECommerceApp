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

/** A saved (favorited) product for a customer, keyed by email so the wishlist syncs across devices. */
@Entity
@Table(name = "wishlist_item", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "product_id"}))
@Getter
@Setter
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
