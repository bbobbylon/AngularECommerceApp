package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * A store hosted on this deployment (roadmap #21, Milestone A). {@code slug} is the subdomain/tenant
 * identifier resolved per-request by {@code TenantResolutionFilter}; {@code active} lets a tenant be
 * suspended (404s its traffic) without deleting its data. {@code plan} is unused for now — reserved
 * for roadmap #22 (tenant billing/plans) so that feature doesn't need its own schema change just to
 * record which plan a tenant is on.
 *
 * <p>{@code Tenant} itself never carries a {@code tenant_id} — it's the one entity that sits above the
 * tenant boundary, not inside it.
 */
@Entity
@Table(name = "tenant", uniqueConstraints = @UniqueConstraint(name = "uk_tenant_slug", columnNames = "slug"))
@Getter
@Setter
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Subdomain / tenant identifier used for request resolution (e.g. "demo" -> demo.example.com). */
    @Column(name = "slug", nullable = false, length = 63)
    private String slug;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Reserved for roadmap #22 (tenant billing/plans); unused for now. */
    @Column(name = "plan")
    private String plan;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
