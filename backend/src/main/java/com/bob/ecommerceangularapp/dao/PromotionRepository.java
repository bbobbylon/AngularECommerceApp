package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Managed via PromotionService (auto-evaluated in TaxShippingService) + AdminPromotionController; not exposed by SDR. */
@RepositoryRestResource(exported = false)
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByActiveTrue();
}
