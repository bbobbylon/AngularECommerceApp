package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dto.AdminStats;
import com.bob.ecommerceangularapp.dto.CategoryRequest;
import com.bob.ecommerceangularapp.dto.CurrentAdminView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.dto.ReviewView;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.service.AdminService;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/** Admin dashboard metrics + category management. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** Same defaults as {@code SecurityConfig} — the known RBAC (roadmap #19) admin-tier roles. */
    @Value("${app.security.admin-role:Admin}")
    private String adminRole;
    @Value("${app.security.order-manager-role:OrderManager}")
    private String orderManagerRole;
    @Value("${app.security.viewer-role:Viewer}")
    private String viewerRole;
    @Value("${app.security.superadmin-role:SuperAdmin}")
    private String superAdminRole;

    private final AdminService adminService;
    private final ProductCategoryRepository productCategoryRepository;
    private final ReviewService reviewService;
    private final AuditLogService auditLogService;

    public AdminController(AdminService adminService,
                          ProductCategoryRepository productCategoryRepository,
                          ReviewService reviewService,
                          AuditLogService auditLogService) {
        this.adminService = adminService;
        this.productCategoryRepository = productCategoryRepository;
        this.reviewService = reviewService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return adminService.stats();
    }

    /**
     * The caller's admin-tier role(s), so the SPA can tailor its UI (e.g. hide actions a Viewer can't
     * perform). Falls back to full {@code Admin} plus {@code SuperAdmin} when the open (no-Okta) chain is
     * active — there's no authentication to derive a role from, and the rest of the app already treats
     * that mode as a full-access local/demo convenience, now including the platform tier (roadmap #21
     * Milestone B) so local dev can reach {@code /platform} without standing up Okta groups.
     */
    @GetMapping("/me")
    public CurrentAdminView me(Authentication authentication) {
        Set<String> known = Set.of(adminRole, orderManagerRole, viewerRole, superAdminRole);
        List<String> roles = authentication == null
                ? List.of()
                : authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(known::contains)
                        .toList();
        return new CurrentAdminView(roles.isEmpty() ? List.of(adminRole, superAdminRole) : roles);
    }

    @GetMapping("/categories")
    public List<ProductCategory> categories() {
        return productCategoryRepository.findAllByTenantId(TenantContext.currentTenantId());
    }

    @PostMapping("/categories")
    public ResponseEntity<ProductCategory> createCategory(Authentication authentication, @Valid @RequestBody CategoryRequest request) {
        ProductCategory saved = adminService.createCategory(request.name());
        auditLogService.record(authentication, "CATEGORY_CREATE", "ProductCategory", String.valueOf(saved.getId()), saved.getCategoryName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/reviews")
    public PageResponse<ReviewView> reviews(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(reviewService.listAll(PageRequest.of(page, size)));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(Authentication authentication, @PathVariable Long id) {
        reviewService.delete(id);
        auditLogService.record(authentication, "REVIEW_DELETE", "Review", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }
}

