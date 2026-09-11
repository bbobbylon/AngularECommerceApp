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

/**
 * Exposed read-only via Spring Data REST (writes disabled by {@code MyDataRestConfig}) and also a
 * {@link JpaSpecificationExecutor} — that half backs {@code ProductQueryService}'s faceted search,
 * which builds its own {@code Specification} rather than using a derived-query method here.
 */
@RepositoryRestResource(collectionResourceRel = "products", path = "products")
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByCategoryId(@Param("id") Long id, Pageable pageable);

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

    // ----- admin back office, tenant-scoped (roadmap #21, Milestone B) -----
    Page<Product> findAllByTenantId(Long tenantId, Pageable pageable);

    // Unpaged variant backs the roadmap-#15 merged inventory view (roadmap #21, Milestone D — replaces
    // an unscoped findAll()/findBySku() that mixed every tenant's stock into one admin inventory screen).
    List<Product> findAllByTenantId(Long tenantId);

    Optional<Product> findBySkuAndTenantId(String sku, Long tenantId);

    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndActiveTrue(Long tenantId);

    long countByTenantIdAndUnitsInStockLessThan(Long tenantId, int threshold);

    long countByTenantIdAndOriginalPriceNotNull(Long tenantId);
}
