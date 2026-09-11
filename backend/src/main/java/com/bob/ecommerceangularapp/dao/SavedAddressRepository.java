package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Account address book — served via the custom account controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could leak a same-email customer's address book across two different storefronts.
    List<SavedAddress> findByEmailIgnoreCaseAndTenantIdOrderByDefaultAddressDescIdDesc(String email, Long tenantId);

    Optional<SavedAddress> findByIdAndTenantId(Long id, Long tenantId);
}
