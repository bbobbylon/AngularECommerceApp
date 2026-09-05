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
 * The single site-wide announcement banner shown above the header (roadmap #17 — simple CMS). Only
 * ever has one row in practice — {@link com.bob.ecommerceangularapp.service.ContentService} upserts
 * it — so the storefront never has to choose between several.
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

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "link_text")
    private String linkText;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
