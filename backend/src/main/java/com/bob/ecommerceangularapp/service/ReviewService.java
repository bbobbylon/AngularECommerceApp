package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ReviewRepository;
import com.bob.ecommerceangularapp.dto.ReviewRequest;
import com.bob.ecommerceangularapp.dto.ReviewSummary;
import com.bob.ecommerceangularapp.dto.ReviewView;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public Page<ReviewView> list(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByDateCreatedDesc(productId, pageable).map(ReviewView::of);
    }

    public Page<ReviewView> listAll(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(ReviewView::of);
    }

    public ReviewSummary summary(Long productId) {
        return summarize(reviewRepository.findByProductId(productId));
    }

    @Transactional
    public ReviewView create(ReviewRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.productId()));

        Review review = new Review();
        review.setProductId(request.productId());
        review.setAuthorName(request.authorName().trim());
        review.setRating(request.rating());
        review.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
        review.setVerifiedBuyer(false);
        Review saved = reviewRepository.save(review);

        recomputeAggregates(product);
        return ReviewView.of(saved);
    }

    @Transactional
    public void delete(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        Long productId = review.getProductId();
        reviewRepository.deleteById(id);
        productRepository.findById(productId).ifPresent(this::recomputeAggregates);
    }

    /** Recompute and persist a product's denormalized averageRating + reviewCount. */
    public void recomputeAggregates(Product product) {
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        if (reviews.isEmpty()) {
            product.setAverageRating(null);
            product.setReviewCount(0);
        } else {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            product.setAverageRating(round1(avg));
            product.setReviewCount(reviews.size());
        }
        productRepository.save(product);
    }

    private ReviewSummary summarize(List<Review> reviews) {
        int[] distribution = new int[5];
        for (Review r : reviews) {
            int index = Math.min(Math.max(r.getRating(), 1), 5) - 1;
            distribution[index]++;
        }
        double avg = reviews.isEmpty() ? 0 : reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return new ReviewSummary(round1(avg), reviews.size(), distribution);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
