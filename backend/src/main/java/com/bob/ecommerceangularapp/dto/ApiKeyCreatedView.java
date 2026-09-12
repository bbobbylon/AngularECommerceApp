package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.ApiKey;

import java.util.Date;

/**
 * Returned exactly once, at issue time — the only place the raw secret ({@code rawKey}) is ever
 * exposed. It is never persisted (only its SHA-256 hash is), so it cannot be recovered afterward;
 * every other read of this key uses {@link ApiKeyView} instead.
 */
public record ApiKeyCreatedView(
        Long id,
        String name,
        String keyPrefix,
        String authorities,
        String rawKey,
        Date expiresAt,
        Date dateCreated) {

    public static ApiKeyCreatedView from(ApiKey apiKey, String rawKey) {
        return new ApiKeyCreatedView(apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(),
                apiKey.getAuthorities(), rawKey, apiKey.getExpiresAt(), apiKey.getDateCreated());
    }
}
