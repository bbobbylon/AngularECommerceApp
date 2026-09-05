package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Warehouses are served through the fulfillment admin controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findAllByOrderByPriorityAscNameAsc();
}
