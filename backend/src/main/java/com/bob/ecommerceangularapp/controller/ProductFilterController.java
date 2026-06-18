package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.dto.ProductCardView;
import com.bob.ecommerceangularapp.dto.ProductVariantView;
import com.bob.ecommerceangularapp.service.ProductQueryService;
import com.bob.ecommerceangularapp.service.ProductVariantService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Faceted catalog search. Separate path from the Spring Data REST /api/products resource so the
 * two don't collide. Returns the stable {@link PageResponse} envelope.
 */
@RestController
@RequestMapping("/api/catalog")
public class ProductFilterController {

    private final ProductQueryService productQueryService;
    private final ProductVariantService productVariantService;

    public ProductFilterController(ProductQueryService productQueryService,
                                   ProductVariantService productVariantService) {
        this.productQueryService = productQueryService;
        this.productVariantService = productVariantService;
    }

    /** Active, purchasable variants for a product (price/image already resolved). Empty for single-SKU items. */
    @GetMapping("/products/{id}/variants")
    public List<ProductVariantView> variants(@PathVariable Long id) {
        return productVariantService.viewsForProduct(id);
    }

    @GetMapping("/search")
    public PageResponse<ProductCardView> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(defaultValue = "") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        PageRequest pageable = PageRequest.of(page, size, parseSort(sort));
        return PageResponse.of(productQueryService.search(
                categoryId, keyword, minPrice, maxPrice, inStock, onSale, minRating, pageable));
    }

    /** "field,dir" → Sort; blank → newest-ish default (id desc). */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("id").ascending();
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
        Sort.Order order = desc ? Sort.Order.desc(field) : Sort.Order.asc(field);
        return Sort.by(order);
    }
}
