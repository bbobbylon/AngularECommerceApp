package com.bob.ecommerceangularapp.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable, self-contained pagination envelope for admin endpoints (avoids serializing Spring's
 * {@code Page} directly, which warns about unstable JSON).
 */
public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(),
                page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
