package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.CacheConfig;
import com.bob.ecommerceangularapp.dao.ApiKeyRepository;
import com.bob.ecommerceangularapp.entity.ApiKey;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Raw API key -&gt; {@link ApiKey} lookup, shared by {@code TenantResolutionFilter} (which resolves
 * tenant identity from the key) and {@code ApiKeyAuthenticationFilter} (which derives Spring
 * Security authorities from it) so a single request only ever hashes/queries once (roadmap #23,
 * Milestone A). Cached like {@link TenantResolutionService} — resolving the caller's key can't be
 * an uncached database hit on every headless request.
 *
 * <p>{@link #lookup} returns the raw cached entity; {@link #isUsable} applies the active/expired/
 * revoked checks fresh on every call (cheap in-memory checks against the already-cached object, not
 * a second query) so both callers see identical, current validity semantics. {@code ApiKeyService}
 * evicts this cache on every mutation, so a revoke takes effect immediately rather than waiting out
 * the TTL.
 */
@Service
public class ApiKeyLookupService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyLookupService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Cacheable(value = CacheConfig.API_KEY_LOOKUP, key = "#rawKey")
    public Optional<ApiKey> lookup(String rawKey) {
        return apiKeyRepository.findByKeyHash(hash(rawKey));
    }

    public boolean isUsable(ApiKey apiKey) {
        if (!apiKey.isActive() || apiKey.getRevokedAt() != null) {
            return false;
        }
        return apiKey.getExpiresAt() == null || apiKey.getExpiresAt().after(new Date());
    }

    public static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
