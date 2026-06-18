package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Saved cards (Stripe references only) — served via the custom account controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {

    List<SavedPaymentMethod> findByEmailIgnoreCaseOrderByDefaultMethodDescIdDesc(String email);
}
