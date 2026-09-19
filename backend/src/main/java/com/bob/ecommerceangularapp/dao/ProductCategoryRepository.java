package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

/** Exposed read-only via Spring Data REST (writes disabled by {@code MyDataRestConfig}); admin CRUD goes through {@code AdminService} instead. */
@RepositoryRestResource(collectionResourceRel = "productCategory", path = "product-category")
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    /** Backs {@code TenantResourceGuardFilter} — closes the SDR {@code findById} gap (roadmap #21).
     * Unexported like every {@code *AndTenantId} method below: all three are called only from Java
     * (which always supplies the ambient {@code TenantContext} tenant), and a caller-controlled
     * {@code tenantId} search param would otherwise let anyone list or probe another tenant's
     * categories directly. */
    @RestResource(exported = false)
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // ----- admin back office, tenant-scoped (roadmap #21, Milestone B) -----
    @RestResource(exported = false)
    List<ProductCategory> findAllByTenantId(Long tenantId);

    @RestResource(exported = false)
    Optional<ProductCategory> findByIdAndTenantId(Long id, Long tenantId);
}
