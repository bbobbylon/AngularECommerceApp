package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.CacheConfig;
import com.bob.ecommerceangularapp.dao.TenantRepository;
import com.bob.ecommerceangularapp.dto.PlatformTenantRequest;
import com.bob.ecommerceangularapp.entity.Tenant;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Platform-level {@link Tenant} management (roadmap #21, Milestone B) — create/list/edit/deactivate
 * the tenants hosted on this deployment. Gated on the {@code SuperAdmin} role in {@code SecurityConfig},
 * not the tenant-scoped {@code Admin}/{@code OrderManager}/{@code Viewer} roles from roadmap #19: a
 * tenant is platform-level data that sits above the tenant boundary, so this service never reads
 * {@link com.bob.ecommerceangularapp.config.TenantContext}.
 *
 * <p>Every mutation evicts {@link CacheConfig#TENANT_LOOKUP}, whole — {@code TenantResolutionService}
 * caches by slug and caches misses too, so a newly-created slug (or a just-reactivated one) would
 * otherwise stay invisible to {@code TenantResolutionFilter} until the cache entry expires.
 */
@Service
public class PlatformTenantService {

    private final TenantRepository tenantRepository;

    public PlatformTenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<Tenant> list() {
        return tenantRepository.findAllByOrderByDisplayNameAsc();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.TENANT_LOOKUP, allEntries = true)
    public Tenant save(PlatformTenantRequest request) {
        String slug = request.slug().trim().toLowerCase();
        boolean slugTaken = request.id() == null
                ? tenantRepository.existsBySlug(slug)
                : tenantRepository.existsBySlugAndIdNot(slug, request.id());
        if (slugTaken) {
            throw new IllegalArgumentException("A tenant with slug \"" + slug + "\" already exists.");
        }

        Tenant tenant = request.id() == null
                ? new Tenant()
                : tenantRepository.findById(request.id())
                        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.id()));
        tenant.setSlug(slug);
        tenant.setDisplayName(request.displayName().trim());
        tenant.setContactEmail(blankToNull(request.contactEmail()));
        tenant.setPlan(blankToNull(request.plan()));
        tenant.setActive(request.active() == null || request.active());
        return tenantRepository.save(tenant);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.TENANT_LOOKUP, allEntries = true)
    public void deactivate(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
