package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The single per-tenant announcement banner shown above the header (roadmap #17 — simple CMS). Only
 * ever has one row per tenant in practice — {@link com.bob.ecommerceangularapp.service.ContentService}
 * upserts it — so the storefront never has to choose between several.
 */
@Entity
@Table(name = "site_banner")
@Getter
@Setter
public class SiteBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy, Milestone C). See {@link Product#getTenantId()} for the isolation rationale. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "link_text")
    private String linkText;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
