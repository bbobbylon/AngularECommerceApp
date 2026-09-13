package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * A tenant-issued credential for headless/programmatic access to its own back office (roadmap #23,
 * Milestone A). {@code keyHash} is the SHA-256 hash of the raw secret — the raw value is generated
 * by {@code ApiKeyService.issue} and returned to the caller exactly once; it is never persisted.
 * {@code keyPrefix} is a short, unhashed slice of the raw secret shown in list views so an admin can
 * tell keys apart afterward. {@code authorities} is a comma-separated list reusing the existing
 * Admin/OrderManager/Viewer role strings (roadmap #19) — no new authority vocabulary.
 */
@Entity
@Table(name = "api_key", uniqueConstraints = @UniqueConstraint(name = "uk_api_key_key_hash", columnNames = "key_hash"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    /** Comma-separated authority strings, e.g. "Admin" or "OrderManager,Viewer". */
    @Column(name = "authorities", nullable = false)
    private String authorities;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private Date expiresAt;

    @Column(name = "last_used_at")
    private Date lastUsedAt;

    @Column(name = "revoked_at")
    private Date revokedAt;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
