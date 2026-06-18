package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.AbandonedCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Abandoned-cart snapshots — served via the custom controller + scheduler, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface AbandonedCartRepository extends JpaRepository<AbandonedCart, Long> {

    Optional<AbandonedCart> findFirstByEmailIgnoreCaseAndRecoveredFalseOrderByIdDesc(String email);

    List<AbandonedCart> findByEmailIgnoreCaseAndRecoveredFalse(String email);

    List<AbandonedCart> findByRecoveredFalseAndRemindedFalseAndLastUpdatedBefore(Date cutoff);
}
