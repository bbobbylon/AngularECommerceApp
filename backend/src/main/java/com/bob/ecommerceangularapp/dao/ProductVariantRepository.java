package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * Variants are NOT a top-level catalog resource — {@code exported = false} keeps Spring Data REST from
 * auto-publishing a {@code /api/productVariants} CRUD surface. Reads go through the public catalog
 * controller, writes through the admin controller.
 */
@RepositoryRestResource(exported = false)
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    // Unscoped by design: used only by the checkout stock decrement, which operates on a variant SKU
    // already chosen from the current tenant's own catalog at cart time.
    Optional<ProductVariant> findBySku(String sku);

    // Tenant-scoped variants back the roadmap-#15 merged inventory view (roadmap #21, Milestone D —
    // replaces an unscoped findAll()/findBySku() that mixed every tenant's stock into one admin screen).
    List<ProductVariant> findByProduct_TenantId(Long tenantId);

    Optional<ProductVariant> findBySkuAndProduct_TenantId(String sku, Long tenantId);

    void deleteByProductId(Long productId);
}
