package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.BillingInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Append-only billing-attempt ledger (roadmap #22) — served via AdminBillingController, not SDR. */
@RepositoryRestResource(exported = false)
public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, Long> {

    Page<BillingInvoice> findAllByTenantIdOrderByAttemptedAtDesc(Long tenantId, Pageable pageable);
}
