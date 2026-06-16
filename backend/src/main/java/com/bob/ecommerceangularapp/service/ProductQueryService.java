package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Faceted catalog search — builds a JPA Specification from optional filters. */
@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> search(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                Boolean inStock, Boolean onSale, Integer minRating, Pageable pageable) {

        List<Specification<Product>> specs = new ArrayList<>();
        // Storefront only shows active products.
        specs.add((root, query, cb) -> cb.isTrue(root.get("active")));

        if (categoryId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (minPrice != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("unitPrice"), minPrice));
        }
        if (maxPrice != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("unitPrice"), maxPrice));
        }
        if (Boolean.TRUE.equals(inStock)) {
            specs.add((root, query, cb) -> cb.greaterThan(root.get("unitsInStock"), 0));
        }
        if (Boolean.TRUE.equals(onSale)) {
            specs.add((root, query, cb) -> cb.isNotNull(root.get("originalPrice")));
        }
        if (minRating != null && minRating > 0) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), (double) minRating));
        }

        return productRepository.findAll(Specification.allOf(specs), pageable);
    }
}
