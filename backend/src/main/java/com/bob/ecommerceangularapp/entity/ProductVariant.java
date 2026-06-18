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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A purchasable variation of a {@link Product} — e.g. a specific colour/size combination — with its
 * own SKU and stock, and an optional price override. Lives in its own {@code product_variant} table
 * (never ALTERs the populated {@code product} table), mirroring the side-table approach used for
 * {@link Product#getAdditionalImages() gallery images}. A product with zero variants behaves exactly
 * as before (single-SKU); products with variants are sold per-variant on the storefront.
 *
 * <p>Hidden from the Spring Data REST surface (see {@code ProductVariantRepository}); reads go through
 * the public {@code /api/catalog/products/{id}/variants} endpoint and writes through the admin API.
 */
@Entity
@Table(name = "product_variant",
        indexes = @Index(name = "idx_product_variant_product", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Owning product. LAZY so catalog/list queries never drag variants in unless asked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Variant-level SKU (unique). The order line records this so fulfilment knows exactly what shipped. */
    @Column(name = "sku")
    private String sku;

    // Two optional option axes — enough for the overwhelming majority of retail (apparel, etc.).
    // Named with a prefix to dodge `size`/`color` being treated as keywords on MySQL/H2.
    @Column(name = "variant_color")
    private String color;

    @Column(name = "variant_size")
    private String size;

    /** Optional price override. Null means "inherit the product's unit price". */
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "units_in_stock")
    private int unitsInStock;

    /** Optional swatch/variant image; falls back to the product image when null. */
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "active")
    private boolean active;

    /** Human label, e.g. "Black / M", "Red", or "M" — blanks dropped. */
    public String label() {
        StringBuilder sb = new StringBuilder();
        if (color != null && !color.isBlank()) {
            sb.append(color.trim());
        }
        if (size != null && !size.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(size.trim());
        }
        return sb.length() == 0 ? (sku == null ? "Variant" : sku) : sb.toString();
    }
}
