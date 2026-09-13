package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** API keys are served via the custom admin controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /** Looked up per-request by {@link com.bob.ecommerceangularapp.service.ApiKeyLookupService}. */
    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findAllByTenantId(Long tenantId);

    Optional<ApiKey> findByIdAndTenantId(Long id, Long tenantId);
}
