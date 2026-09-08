package com.bob.ecommerceangularapp.dto;

import com.bob.ecommerceangularapp.entity.AuditLogEntry;

import java.util.Date;

/** One row of the global admin audit log (roadmap #19) — powers the admin Audit Log page. */
public record AuditLogView(Long id, String actor, String action, String entityType, String entityId,
                            String details, Date createdAt) {

    public static AuditLogView from(AuditLogEntry entry) {
        return new AuditLogView(entry.getId(), entry.getActor(), entry.getAction(), entry.getEntityType(),
                entry.getEntityId(), entry.getDetails(), entry.getCreatedAt());
    }
}
