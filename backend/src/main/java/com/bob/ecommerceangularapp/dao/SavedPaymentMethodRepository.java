package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Saved cards (Stripe references only) — served via the custom account controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could leak a same-email customer's saved cards across two different storefronts.
    List<SavedPaymentMethod> findByEmailIgnoreCaseAndTenantIdOrderByDefaultMethodDescIdDesc(String email, Long tenantId);

    Optional<SavedPaymentMethod> findByIdAndTenantId(Long id, Long tenantId);
}
