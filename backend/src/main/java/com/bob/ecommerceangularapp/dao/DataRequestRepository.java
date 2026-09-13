package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.DataRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Data-subject requests are served via the custom privacy controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface DataRequestRepository extends JpaRepository<DataRequest, Long> {

    /**
     * Token lookup is deliberately <em>not</em> tenant-scoped: the token is a 256-bit secret that
     * already identifies exactly one row, and the visitor clicking a link from their inbox has no
     * tenant header to send. The tenant is read back off the row instead.
     */
    Optional<DataRequest> findByToken(String token);

    /** Open requests of one type for one subject — used to avoid stacking duplicates. */
    List<DataRequest> findByEmailIgnoreCaseAndRequestTypeAndStatusAndTenantId(
            String email, String requestType, String status, Long tenantId);

    List<DataRequest> findByEmailIgnoreCaseAndTenantIdOrderByIdDesc(String email, Long tenantId);
}
