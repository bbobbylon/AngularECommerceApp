package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/** Not exposed over REST — tenants are platform-level, not tenant-scoped data (Milestone B/C adds admin access). */
@RepositoryRestResource(exported = false)
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);
}
