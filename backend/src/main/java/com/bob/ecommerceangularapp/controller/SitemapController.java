package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.service.SitemapService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves sitemap.xml (roadmap #11 — SEO), outside the {@code /api} base path since it's a
 * crawler-facing document, not part of the JSON API. Public — no auth, matching robots.txt norms.
 */
@RestController
public class SitemapController {

    private final SitemapService sitemapService;

    public SitemapController(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        return sitemapService.buildSitemapXml();
    }
}
