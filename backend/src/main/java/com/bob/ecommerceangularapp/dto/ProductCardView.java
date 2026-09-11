package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.Product;

import java.math.BigDecimal;

/**
 * Lightweight product projection for the catalog grid. Deliberately omits the lazy
 * {@code additionalImages} gallery (only the details page needs it) — which means list responses
 * carry no lazy associations, so they can be safely cached and never trigger an N+1. The JSON field
 * names match what the Angular product cards read; the details page still uses the full SDR
 * {@code /products/{id}} resource for the gallery.
 */
public record ProductCardView(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal unitPrice,
        BigDecimal originalPrice,
        String imageUrl,
        boolean active,
        int unitsInStock,
        Double averageRating,
        Integer reviewCount) {

    public static ProductCardView of(Product p) {
        return new ProductCardView(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getUnitPrice(), p.getOriginalPrice(), p.getImageUrl(),
                p.isActive(), p.getUnitsInStock(), p.getAverageRating(), p.getReviewCount());
    }
}
