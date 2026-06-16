package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/** A customer product review. Linked to a product by id (no FK, mirroring OrderItem). */
@Entity
@Table(name = "review")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "rating")
    private int rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "verified_buyer")
    private boolean verifiedBuyer;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
