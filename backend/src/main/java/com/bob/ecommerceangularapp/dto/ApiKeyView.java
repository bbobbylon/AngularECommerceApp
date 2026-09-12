package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.ApiKey;

import java.util.Date;

/** List-view of an issued API key — never carries the raw secret or its hash. */
public record ApiKeyView(
        Long id,
        String name,
        String keyPrefix,
        String authorities,
        boolean active,
        Date expiresAt,
        Date lastUsedAt,
        Date revokedAt,
        Date dateCreated) {

    public static ApiKeyView from(ApiKey apiKey) {
        return new ApiKeyView(apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(), apiKey.getAuthorities(),
                apiKey.isActive(), apiKey.getExpiresAt(), apiKey.getLastUsedAt(), apiKey.getRevokedAt(),
                apiKey.getDateCreated());
    }
}
