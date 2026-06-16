package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.math.BigDecimal;

/**
 * Order history. The collection path /api/orders/** is protected by SecurityConfig
 * once an Okta issuer URI is configured (Milestone 3).
 */
@RepositoryRestResource(collectionResourceRel = "orders", path = "orders")
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCustomerEmailOrderByDateCreatedDesc(@Param("email") String email, Pageable pageable);

    // ----- admin -----
    Page<Order> findAllByOrderByDateCreatedDesc(Pageable pageable);

    @Query("select coalesce(sum(o.totalPrice), 0) from Order o")
    BigDecimal sumTotalRevenue();
}
