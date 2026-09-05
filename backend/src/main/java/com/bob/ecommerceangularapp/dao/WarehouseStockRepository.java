package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Per-warehouse stock rows are served through the fulfillment admin controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    List<WarehouseStock> findByWarehouseId(Long warehouseId);

    Optional<WarehouseStock> findByWarehouseIdAndSku(Long warehouseId, String sku);

    void deleteByWarehouseId(Long warehouseId);
}
