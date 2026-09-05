package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.InventoryAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Audit log — served via the admin controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    Page<InventoryAdjustment> findAllByOrderByDateCreatedDesc(Pageable pageable);
}
