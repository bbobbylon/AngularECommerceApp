package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dto.AdminStats;
import com.bob.ecommerceangularapp.dto.CategoryRequest;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.dto.ReviewView;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.service.AdminService;
import com.bob.ecommerceangularapp.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin dashboard metrics + category management. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ProductCategoryRepository productCategoryRepository;
    private final ReviewService reviewService;

    public AdminController(AdminService adminService,
                          ProductCategoryRepository productCategoryRepository,
                          ReviewService reviewService) {
        this.adminService = adminService;
        this.productCategoryRepository = productCategoryRepository;
        this.reviewService = reviewService;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return adminService.stats();
    }

    @GetMapping("/categories")
    public List<ProductCategory> categories() {
        return productCategoryRepository.findAll();
    }

    @PostMapping("/categories")
    public ResponseEntity<ProductCategory> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCategory(request.name()));
    }

    @GetMapping("/reviews")
    public PageResponse<ReviewView> reviews(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(reviewService.listAll(PageRequest.of(page, size)));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

