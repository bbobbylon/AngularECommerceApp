package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * A single recorded admin action (roadmap #19) — who did what, to which entity, and when. Written by
 * {@code AuditLogService.record(...)} from admin controllers; never updated or deleted, only appended.
 */
@Entity
@Table(name = "audit_log_entry",
        indexes = {
                @Index(name = "idx_audit_log_entity_type", columnList = "entity_type"),
                @Index(name = "idx_audit_log_created_at", columnList = "created_at"),
        })
@Getter
@Setter
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Resolved from the request's authenticated principal name, or "anonymous" without one. */
    @Column(name = "actor")
    private String actor;

    /** Short verb-noun code, e.g. "PRODUCT_UPDATE", "ORDER_STATUS_UPDATE". */
    @Column(name = "action")
    private String action;

    /** e.g. "Product", "Order", "Coupon". */
    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "created_at")
    @CreationTimestamp
    private Date createdAt;
}
