package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Warehouses are served through the fulfillment admin controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    // ----- tenant-scoped (roadmap #21, Milestone D) -----
    List<Warehouse> findAllByTenantIdOrderByPriorityAscNameAsc(Long tenantId);

    Optional<Warehouse> findByIdAndTenantId(Long id, Long tenantId);
}
