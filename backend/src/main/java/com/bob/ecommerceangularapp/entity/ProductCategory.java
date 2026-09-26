package com.bob.ecommerceangularapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.HashSet;
import java.util.Set;

/** A catalog category ({@code Product.category}, {@code @ManyToOne}). Tenant-scoped since roadmap #21 Milestone A. */
@Entity
@Table(name = "product_category")
@Getter
@Setter
@NoArgsConstructor
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy) — see {@link Product#getTenantId()}. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "category_name")
    private String categoryName;

    // @JsonIgnore only suppresses this collection from the EMBEDDED product-category representation —
    // it does nothing to Spring Data REST's separate "related resource" endpoint for the association,
    // GET /api/product-category/{id}/products, which stayed live and unguarded. Confirmed as a real,
    // live, cross-tenant leak via a real-filter-chain probe (2026-09-26): TenantResourceGuardFilter
    // correctly 404s GET /api/product-category/{id} for a category id belonging to another tenant, but
    // the sibling GET /api/product-category/{id}/products link for that SAME id was never in the
    // guard's pattern list, so it returned 200 with that other tenant's real product (name/SKU/price/
    // tenantId all visible) to a caller with no tenant header at all. Product's own repository is
    // exported (unlike Customer's, whose @RepositoryRestResource(exported = false) already makes
    // Order's related "customer" link 404 with no extra annotation needed), so this association needed
    // its own explicit suppression. No live caller: the category sidebar / filter dropdown moved to the
    // tenant-scoped GET /api/catalog/categories before this was found (see ProductFilterController).
    @OneToMany(mappedBy = "category")
    @JsonIgnore
    @RestResource(exported = false)
    private Set<Product> products = new HashSet<>();

    public ProductCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    /** Maintains both sides of the relationship. */
    public void add(Product product) {
        if (product != null) {
            products.add(product);
            product.setCategory(this);
        }
    }
}
