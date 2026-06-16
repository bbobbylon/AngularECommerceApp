package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.Review;

import java.util.Date;

/** Review row for product pages + admin moderation. */
public record ReviewView(
        Long id,
        Long productId,
        String authorName,
        int rating,
        String comment,
        boolean verifiedBuyer,
        Date dateCreated) {

    public static ReviewView of(Review r) {
        return new ReviewView(r.getId(), r.getProductId(), r.getAuthorName(), r.getRating(),
                r.getComment(), r.isVerifiedBuyer(), r.getDateCreated());
    }
}
