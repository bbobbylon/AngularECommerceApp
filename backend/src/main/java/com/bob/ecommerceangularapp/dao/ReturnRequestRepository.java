package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Returns are served through the custom return/admin controllers, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findByCustomerEmailIgnoreCaseOrderByDateCreatedDesc(String email);

    List<ReturnRequest> findByOrderId(Long orderId);

    List<ReturnRequest> findAllByOrderByDateCreatedDesc();
}
