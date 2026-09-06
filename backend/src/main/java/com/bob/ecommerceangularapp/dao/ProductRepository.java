package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "products", path = "products")
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByCategoryId(@Param("id") Long id, Pageable pageable);

    Optional<Product> findBySku(String sku);

    Page<Product> findByNameContaining(@Param("name") String name, Pageable pageable);

    /** On-sale products — anything with a pre-sale ("was") price set. Powers the /sale page. */
    Page<Product> findByOriginalPriceNotNull(Pageable pageable);

    /** All active products, unpaged — powers sitemap.xml generation. */
    List<Product> findByActiveTrue();

    // ----- admin dashboard metrics -----
    long countByActiveTrue();

    long countByUnitsInStockLessThan(int threshold);

    long countByOriginalPriceNotNull();

    /** Backs {@code TenantResourceGuardFilter} — closes the SDR {@code findById} gap (roadmap #21). */
    boolean existsByIdAndTenantId(Long id, Long tenantId);
}
