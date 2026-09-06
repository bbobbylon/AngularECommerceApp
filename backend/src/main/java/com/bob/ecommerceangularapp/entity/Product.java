package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
// The storefront filters on active products within a category on nearly every catalog query.
@Table(name = "product", indexes = @Index(name = "idx_product_active_category", columnList = "active, category_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Roadmap #21 (multi-tenancy, Milestone A). Isolation is enforced explicitly — services add a
     * {@code tenant_id} predicate to every query/specification, and {@code TenantResourceGuardFilter}
     * closes the {@code findById}-shaped gap (SDR item resources) that a query-level predicate can't
     * reach. (Hibernate's {@code @TenantId} discriminator was evaluated as a secondary safety net but
     * dropped: it requires every {@code EntityManager} in the app — including ones Spring Data JPA
     * opens at repository-bootstrap time, for entities with no tenant dimension at all — to resolve a
     * tenant id up front, which breaks on a fresh/empty database before any tenant row exists.)
     */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    /**
     * Pre-sale ("was") price. Null for full-price items. When set and greater than
     * {@link #unitPrice}, the product is on sale and the UI shows a strikethrough + % off.
     */
    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Extra gallery images (thumbnails on the product page). Stored in a side table so it never
     * ALTERs the populated {@code product} table — safe under MySQL strict mode. Lazy + batched so
     * list endpoints don't pay an N+1; open-in-view (default) serializes it on the details page.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_image", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url")
    @BatchSize(size = 60)
    @Builder.Default
    private List<String> additionalImages = new ArrayList<>();

    @Column(name = "active")
    private boolean active;

    @Column(name = "units_in_stock")
    private int unitsInStock;

    // Denormalized review aggregates (kept in sync by ReviewService) so cards/grids show ratings
    // without an N+1. Nullable wrappers on purpose — adding a NOT NULL column to a populated table
    // fails under MySQL strict mode (see docs/MAINTENANCE.md).
    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;

    @Column(name = "last_updated")
    @UpdateTimestamp
    private Date lastUpdated;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ProductCategory category;
}
