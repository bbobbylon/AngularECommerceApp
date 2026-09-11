package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.WishlistItemRepository;
import com.bob.ecommerceangularapp.dto.WishlistSyncRequest;
import com.bob.ecommerceangularapp.entity.WishlistItem;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Account wishlist, keyed by email so favorites sync across devices. */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistItemRepository wishlistRepository;

    public WishlistController(WishlistItemRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    @GetMapping
    public List<Long> get(@RequestParam String email) {
        return productIds(normalize(email));
    }

    /** Merge the device's local ids into the saved wishlist, then return the union. */
    @PostMapping("/sync")
    @Transactional
    public List<Long> sync(@Valid @RequestBody WishlistSyncRequest request) {
        String email = normalize(request.email());
        Long tenantId = TenantContext.currentTenantId();
        if (request.productIds() != null) {
            for (Long productId : request.productIds()) {
                if (productId != null && !wishlistRepository.existsByEmailAndProductIdAndTenantId(email, productId, tenantId)) {
                    WishlistItem item = new WishlistItem();
                    item.setTenantId(tenantId);
                    item.setEmail(email);
                    item.setProductId(productId);
                    wishlistRepository.save(item);
                }
            }
        }
        return productIds(email);
    }

    @DeleteMapping
    @Transactional
    public void remove(@RequestParam String email, @RequestParam Long productId) {
        wishlistRepository.deleteByEmailAndProductIdAndTenantId(normalize(email), productId, TenantContext.currentTenantId());
    }

    private List<Long> productIds(String email) {
        return wishlistRepository.findByEmailAndTenantId(email, TenantContext.currentTenantId()).stream()
                .map(WishlistItem::getProductId)
                .distinct()
                .sorted()
                .toList();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
