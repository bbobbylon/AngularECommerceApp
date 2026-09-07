package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Shipments are served through the fulfillment controllers, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    // orderId is only ever resolved through a tenant-scoped or email-verified Order lookup first
    // (see FulfillmentService.shipmentsForOrder/trackShipments), so this itself stays unscoped.
    List<Shipment> findByOrderIdOrderByDateCreatedDesc(Long orderId);

    List<Shipment> findByOrderTrackingNumberOrderByDateCreatedDesc(String orderTrackingNumber);

    boolean existsByWarehouseId(Long warehouseId);

    // ----- tenant-scoped ownership check (roadmap #21, Milestone D) -----
    Optional<Shipment> findByIdAndTenantId(Long id, Long tenantId);
}
