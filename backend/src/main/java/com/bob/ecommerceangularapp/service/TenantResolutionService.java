package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.CacheConfig;
import com.bob.ecommerceangularapp.dao.TenantRepository;
import com.bob.ecommerceangularapp.entity.Tenant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Slug -&gt; {@link Tenant} lookup for {@code TenantResolutionFilter} (roadmap #21, Milestone A).
 * Cached (short TTL, same cadence as the catalog-search cache) so resolving the current tenant isn't
 * an uncached database hit on every single request.
 */
@Service
public class TenantResolutionService {

    private final TenantRepository tenantRepository;

    public TenantResolutionService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Cacheable(value = CacheConfig.TENANT_LOOKUP, key = "#slug")
    public Optional<Tenant> resolveBySlug(String slug) {
        return tenantRepository.findBySlugAndActiveTrue(slug);
    }
}
