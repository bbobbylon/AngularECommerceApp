package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/**
 * Not exposed over REST — tenants are platform-level, not tenant-scoped data. Admin access is via
 * {@code PlatformTenantController} (roadmap #21, Milestone B), gated on the {@code SuperAdmin} role.
 */
@RepositoryRestResource(exported = false)
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Tenant> findAllByOrderByDisplayNameAsc();
}
