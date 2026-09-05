package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.AuditLogRepository;
import com.bob.ecommerceangularapp.dto.AuditLogView;
import com.bob.ecommerceangularapp.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Global admin audit log (roadmap #19): one row per admin mutation, independent of the
 * domain-specific {@code InventoryAdjustment} ledger. Actor identity is resolved from whatever
 * {@link Authentication} the request carried — the JWT subject when Okta is configured, or
 * "anonymous" when the app is running its open (no-Okta) dev chain, so this degrades gracefully the
 * same way the rest of the app's optional integrations do.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Authentication authentication, String action, String entityType, String entityId, String details) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setActor(resolveActor(authentication));
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    public Page<AuditLogView> list(Pageable pageable, String entityType) {
        Page<AuditLogEntry> page = (entityType == null || entityType.isBlank())
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        return page.map(AuditLogView::from);
    }

    private static String resolveActor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
