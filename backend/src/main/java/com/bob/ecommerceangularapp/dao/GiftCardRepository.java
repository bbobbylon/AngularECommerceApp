package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.GiftCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/** Gift cards are served via the custom checkout/admin controllers, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface GiftCardRepository extends JpaRepository<GiftCard, Long> {

    Optional<GiftCard> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
