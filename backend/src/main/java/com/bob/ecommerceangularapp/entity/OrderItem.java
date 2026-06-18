package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "product_id")
    private Long productId;

    /** SKU of the chosen variant, if the product was bought by variant (null for single-SKU products). */
    @Column(name = "variant_sku")
    private String variantSku;

    /** Human label of the chosen variant (e.g. "Black / M") — kept on the line for fulfilment + history. */
    @Column(name = "variant_label")
    private String variantLabel;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
