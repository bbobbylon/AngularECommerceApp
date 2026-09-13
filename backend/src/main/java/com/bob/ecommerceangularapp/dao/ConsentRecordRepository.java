package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Consent records are served via the custom privacy controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    /**
     * The visitor's current choice is simply their most recent one — the ledger is append-only, so
     * "latest row wins" is the read model. Ordered by id rather than {@code dateCreated} because
     * two decisions made in the same millisecond must still have a defined order.
     */
    Optional<ConsentRecord> findFirstByVisitorIdAndTenantIdOrderByIdDesc(String visitorId, Long tenantId);

    /** Every decision this visitor ever made — included in their data export. */
    List<ConsentRecord> findByVisitorIdAndTenantIdOrderByIdDesc(String visitorId, Long tenantId);

    /** Decisions we were able to attach to a known email — the export/erasure lookup path. */
    List<ConsentRecord> findByEmailIgnoreCaseAndTenantIdOrderByIdDesc(String email, Long tenantId);
}
