package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.CacheConfig;
import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ApiKeyRepository;
import com.bob.ecommerceangularapp.dto.AdminApiKeyRequest;
import com.bob.ecommerceangularapp.dto.ApiKeyCreatedView;
import com.bob.ecommerceangularapp.entity.ApiKey;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * Issues/lists/revokes tenant-scoped API keys (roadmap #23, Milestone A). The raw secret is
 * generated with {@link SecureRandom}, hashed (SHA-256, via {@link ApiKeyLookupService#hash}) before
 * persisting, and returned to the caller exactly once at issue time — mirrors this codebase's
 * existing {@code SecureRandom}-based generation idiom ({@code GiftCardService}/{@code
 * ReferralService}), extended with hashing since (unlike a gift-card code) this value is a bearer
 * credential. Every mutation evicts {@link CacheConfig#API_KEY_LOOKUP} whole, matching
 * {@code PlatformTenantService}'s precedent for {@link CacheConfig#TENANT_LOOKUP} — a revoked key
 * must stop working immediately, not after the cache's TTL lapses.
 */
@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String KEY_PREFIX_TAG = "lsk_";
    private static final int PREFIX_DISPLAY_LENGTH = 16;

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listAll() {
        return apiKeyRepository.findAllByTenantId(TenantContext.currentTenantId());
    }

    @Transactional
    @CacheEvict(value = CacheConfig.API_KEY_LOOKUP, allEntries = true)
    public ApiKeyCreatedView issue(AdminApiKeyRequest request) {
        Long tenantId = TenantContext.currentTenantId();
        String rawKey = generateRawKey();
        ApiKey apiKey = ApiKey.builder()
                .tenantId(tenantId)
                .name(request.name().trim())
                .keyPrefix(rawKey.substring(0, PREFIX_DISPLAY_LENGTH))
                .keyHash(ApiKeyLookupService.hash(rawKey))
                .authorities(String.join(",", request.authorities()))
                .active(true)
                .expiresAt(request.expiresAt())
                .build();
        ApiKey saved = apiKeyRepository.save(apiKey);
        return ApiKeyCreatedView.from(saved, rawKey);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.API_KEY_LOOKUP, allEntries = true)
    public void revoke(Long id) {
        ApiKey apiKey = apiKeyRepository.findByIdAndTenantId(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
        apiKey.setActive(false);
        apiKey.setRevokedAt(new Date());
        apiKeyRepository.save(apiKey);
    }

    /** {@code lsk_} tag (so a leaked key is recognizable in logs/scans) + 32 random bytes as hex. */
    private String generateRawKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return KEY_PREFIX_TAG + HexFormat.of().formatHex(bytes);
    }
}
