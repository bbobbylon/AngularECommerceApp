package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dao.WishlistItemRepository;
import com.bob.ecommerceangularapp.dto.WishlistSyncRequest;
import com.bob.ecommerceangularapp.entity.WishlistItem;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Account wishlist, keyed by email so favorites sync across devices. */
@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
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
        if (request.productIds() != null) {
            for (Long productId : request.productIds()) {
                if (productId != null && !wishlistRepository.existsByEmailAndProductId(email, productId)) {
                    WishlistItem item = new WishlistItem();
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
        wishlistRepository.deleteByEmailAndProductId(normalize(email), productId);
    }

    private List<Long> productIds(String email) {
        return wishlistRepository.findByEmail(email).stream()
                .map(WishlistItem::getProductId)
                .distinct()
                .sorted()
                .toList();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
