package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Returns are served through the custom return/admin controllers, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could leak a same-email customer's return history across two different storefronts.
    List<ReturnRequest> findByCustomerEmailIgnoreCaseAndTenantIdOrderByDateCreatedDesc(String email, Long tenantId);

    // Unscoped by design: orderId already belongs to exactly one tenant (used only for the
    // duplicate-open-return guard on an order already resolved via its own tenant-agnostic tracking number).
    List<ReturnRequest> findByOrderId(Long orderId);

    List<ReturnRequest> findAllByTenantIdOrderByDateCreatedDesc(Long tenantId);

    Optional<ReturnRequest> findByIdAndTenantId(Long id, Long tenantId);
}
