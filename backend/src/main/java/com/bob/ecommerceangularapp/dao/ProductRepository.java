package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

/**
 * Exposed read-only via Spring Data REST (writes disabled by {@code MyDataRestConfig}) and also a
 * {@link JpaSpecificationExecutor} — that half backs {@code ProductQueryService}'s faceted search,
 * which builds its own {@code Specification} rather than using a derived-query method here.
 */
@RepositoryRestResource(collectionResourceRel = "products", path = "products")
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // These three predate the faceted-search unification (roadmap "Feature set") and are superseded
    // by ProductQueryService's tenant-scoped /api/catalog/search — no frontend code calls them anymore.
    // Left exported, each was a live, unauthenticated, cross-tenant catalog leak (no tenant predicate
    // at all), so all three are unexported rather than deleted outright, in case something still needs
    // reviving as a tenant-scoped equivalent.
    @RestResource(exported = false)
    Page<Product> findByCategoryId(@Param("id") Long id, Pageable pageable);

    @RestResource(exported = false)
    Page<Product> findByNameContaining(@Param("name") String name, Pageable pageable);

    /** On-sale products — anything with a pre-sale ("was") price set. Called only from Java
     * ({@code NewsletterService}'s weekly-ad picks, {@code DataLoader}'s seed check) — the public
     * /sale page goes through the tenant-scoped {@code ProductQueryService} instead. */
    @RestResource(exported = false)
    Page<Product> findByOriginalPriceNotNull(Pageable pageable);

    /** All active products, unpaged — powers sitemap.xml generation (Java-only call site;
     * unexported so it can't be hit directly as an unscoped, cross-tenant catalog dump). */
    @RestResource(exported = false)
    List<Product> findByActiveTrue();

    // ----- admin dashboard metrics -----
    long countByActiveTrue();

    long countByUnitsInStockLessThan(int threshold);

    long countByOriginalPriceNotNull();

    /**
     * Backs {@code TenantResourceGuardFilter} — closes the SDR {@code findById} gap (roadmap #21).
     * Unexported: a caller-supplied {@code tenantId} would otherwise let anyone probe another
     * tenant's product ids directly via {@code /api/products/search/existsByIdAndTenantId}, bypassing
     * the ambient {@link TenantContext} this whole mechanism exists to enforce.
     */
    @RestResource(exported = false)
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // ----- admin back office, tenant-scoped (roadmap #21, Milestone B) -----
    // Both overloads must stay unexported: SDR maps search methods by name only, so two
    // findAllByTenantId methods (regardless of signature) collide on the same /findAllByTenantId
    // search path and throw IllegalStateException("Ambiguous search mapping detected") on first
    // request to the products collection resource.
    @RestResource(exported = false)
    Page<Product> findAllByTenantId(Long tenantId, Pageable pageable);

    // Unpaged variant backs the roadmap-#15 merged inventory view (roadmap #21, Milestone D — replaces
    // an unscoped findAll()/findBySku() that mixed every tenant's stock into one admin inventory screen).
    @RestResource(exported = false)
    List<Product> findAllByTenantId(Long tenantId);

    // Every *AndTenantId method below is called only from Java service code (AdminService,
    // ProductVariantService, ReviewService, ...), which always supplies TenantContext.currentTenantId()
    // — never a caller-controlled value. Left exported, a caller could pass any tenantId directly as a
    // search-resource query param (e.g. /api/products/search/findByIdAndTenantId?id=1&tenantId=2) and
    // read another tenant's catalog straight through, bypassing TenantContext/TenantResolutionFilter
    // entirely. @RestResource(exported = false) closes that off with zero change to the Java call sites.
    @RestResource(exported = false)
    Optional<Product> findBySkuAndTenantId(String sku, Long tenantId);

    @RestResource(exported = false)
    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);

    @RestResource(exported = false)
    long countByTenantId(Long tenantId);

    @RestResource(exported = false)
    long countByTenantIdAndActiveTrue(Long tenantId);

    @RestResource(exported = false)
    long countByTenantIdAndUnitsInStockLessThan(Long tenantId, int threshold);

    @RestResource(exported = false)
    long countByTenantIdAndOriginalPriceNotNull(Long tenantId);
}
