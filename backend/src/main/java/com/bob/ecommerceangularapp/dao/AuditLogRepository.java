package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Not a public Spring Data REST surface — read only via {@code AdminAuditLogController}. */
@RepositoryRestResource(exported = false)
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    // ----- tenant-scoped (roadmap #21, Milestone D) -----
    Page<AuditLogEntry> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<AuditLogEntry> findByTenantIdAndEntityTypeOrderByCreatedAtDesc(Long tenantId, String entityType, Pageable pageable);
}
