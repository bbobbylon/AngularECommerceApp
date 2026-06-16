package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Managed via WishlistController; not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByEmail(String email);

    boolean existsByEmailAndProductId(String email, Long productId);

    void deleteByEmailAndProductId(String email, Long productId);
}
