package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Managed via WishlistController; not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could merge/leak a same-email customer's wishlist across two different storefronts.
    List<WishlistItem> findByEmailAndTenantId(String email, Long tenantId);

    boolean existsByEmailAndProductIdAndTenantId(String email, Long productId, Long tenantId);

    void deleteByEmailAndProductIdAndTenantId(String email, Long productId, Long tenantId);
}
