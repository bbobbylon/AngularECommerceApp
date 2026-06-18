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

    Optional<ProductVariant> findBySku(String sku);

    void deleteByProductId(Long productId);
}
