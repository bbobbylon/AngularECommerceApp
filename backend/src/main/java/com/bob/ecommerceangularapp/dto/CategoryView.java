package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.ProductCategory;

/**
 * Public, tenant-scoped category read shape for the storefront (category sidebar + product-list
 * filter dropdown). Deliberately omits {@code tenantId} — mirrors {@link ProductCardView}'s
 * precedent of never putting the internal tenant id on a response an anonymous visitor can read.
 * Backs {@code GET /api/catalog/categories}, which replaced the raw Spring Data REST collection
 * resource {@code GET /api/product-category} (see {@code MyDataRestConfig} and
 * {@code docs/SECURITY.md} — that resource had no tenant predicate at all, so it mixed every
 * tenant's categories into one response).
 */
public record CategoryView(Long id, String categoryName) {

    public static CategoryView of(ProductCategory category) {
        return new CategoryView(category.getId(), category.getCategoryName());
    }
}
