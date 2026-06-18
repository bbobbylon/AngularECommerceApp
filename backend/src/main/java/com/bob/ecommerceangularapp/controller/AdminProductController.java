package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminProductRequest;
import com.bob.ecommerceangularapp.dto.AdminVariantRequest;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.service.AdminService;
import com.bob.ecommerceangularapp.service.ProductVariantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin product CRUD. Custom endpoints because Spring Data REST writes are disabled on Product. */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminService adminService;
    private final ProductVariantService variantService;

    public AdminProductController(AdminService adminService, ProductVariantService variantService) {
        this.adminService = adminService;
        this.variantService = variantService;
    }

    @GetMapping
    public PageResponse<Product> list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(adminService.listProducts(PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return adminService.getProduct(id);
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody AdminProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createProduct(request));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody AdminProductRequest request) {
        return adminService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ----- variants (SKU-level inventory) -----

    @GetMapping("/{id}/variants")
    public List<AdminVariantRequest> listVariants(@PathVariable Long id) {
        return variantService.adminListForProduct(id);
    }

    /** Replaces the product's full variant set (upsert by id, delete omitted). */
    @PutMapping("/{id}/variants")
    public List<AdminVariantRequest> replaceVariants(@PathVariable Long id,
                                                     @RequestBody List<@Valid AdminVariantRequest> variants) {
        return variantService.replaceVariants(id, variants);
    }
}
