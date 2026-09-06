package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.GiftCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Gift cards are served via the custom checkout/admin controllers, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface GiftCardRepository extends JpaRepository<GiftCard, Long> {

    Optional<GiftCard> findByCodeIgnoreCaseAndTenantId(String code, Long tenantId);

    boolean existsByCodeIgnoreCaseAndTenantId(String code, Long tenantId);

    List<GiftCard> findAllByTenantId(Long tenantId);

    Optional<GiftCard> findByIdAndTenantId(Long id, Long tenantId);
}
