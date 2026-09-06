package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "productCategory", path = "product-category")
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    /** Backs {@code TenantResourceGuardFilter} — closes the SDR {@code findById} gap (roadmap #21). */
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // ----- admin back office, tenant-scoped (roadmap #21, Milestone B) -----
    List<ProductCategory> findAllByTenantId(Long tenantId);

    Optional<ProductCategory> findByIdAndTenantId(Long id, Long tenantId);
}
