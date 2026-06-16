package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dto.AdminStats;
import com.bob.ecommerceangularapp.dto.CategoryRequest;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin dashboard metrics + category management. */
@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ProductCategoryRepository productCategoryRepository;

    public AdminController(AdminService adminService, ProductCategoryRepository productCategoryRepository) {
        this.adminService = adminService;
        this.productCategoryRepository = productCategoryRepository;
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
}
