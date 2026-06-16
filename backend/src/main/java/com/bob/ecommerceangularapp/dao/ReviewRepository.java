package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Reviews are managed via ReviewController (create) + ReviewService (aggregates); not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdOrderByDateCreatedDesc(Long productId, Pageable pageable);

    long countByProductId(Long productId);

    /** Average rating for a product, or null when it has no reviews. */
    java.util.List<Review> findByProductId(Long productId);
}
