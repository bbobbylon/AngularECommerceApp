package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Shipping methods are admin-managed config — hidden from Spring Data REST; read via the checkout API. */
@RepositoryRestResource(exported = false)
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Long> {

    List<ShippingMethod> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<ShippingMethod> findByCode(String code);
}
