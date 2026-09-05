package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Not a public Spring Data REST surface — read only via {@code AdminAuditLogController}. */
@RepositoryRestResource(exported = false)
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    Page<AuditLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLogEntry> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);
}
