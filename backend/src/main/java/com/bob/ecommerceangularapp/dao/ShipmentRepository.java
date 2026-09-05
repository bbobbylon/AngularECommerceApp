package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Shipments are served through the fulfillment controllers, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByOrderIdOrderByDateCreatedDesc(Long orderId);

    List<Shipment> findByOrderTrackingNumberOrderByDateCreatedDesc(String orderTrackingNumber);

    boolean existsByWarehouseId(Long warehouseId);
}
