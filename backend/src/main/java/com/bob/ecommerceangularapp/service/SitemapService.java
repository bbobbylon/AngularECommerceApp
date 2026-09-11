package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Builds sitemap.xml for the storefront (roadmap #11 — SEO). The frontend is a plain
 * client-side SPA with no server-render pass, so there's no route list to crawl at build time;
 * the backend already holds the live catalog, so it generates the sitemap from current data
 * instead of a stale build-time snapshot. URLs in the sitemap point at the frontend (the pages
 * a crawler should actually visit), even though this endpoint is served by the backend.
 */
@Service
public class SitemapService {

    private static final String[] STATIC_PATHS = {
            "/products", "/sale", "/about", "/faq", "/contact", "/shipping-returns", "/privacy", "/terms",
    };

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final String frontendUrl;

    public SitemapService(ProductRepository productRepository,
                           ProductCategoryRepository productCategoryRepository,
                           @Value("${app.frontend-url:http://localhost:4250}") String frontendUrl) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public String buildSitemapXml() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(xml, frontendUrl + "/", "1.0", null);
        for (String path : STATIC_PATHS) {
            appendUrl(xml, frontendUrl + path, "0.7", null);
        }

        List<ProductCategory> categories = productCategoryRepository.findAll();
        for (ProductCategory category : categories) {
            appendUrl(xml, frontendUrl + "/category/" + category.getId(), "0.6", null);
        }

        List<Product> products = productRepository.findByActiveTrue();
        for (Product product : products) {
            String lastMod = product.getLastUpdated() != null ? dateFormat.format(product.getLastUpdated()) : null;
            appendUrl(xml, frontendUrl + "/products/" + product.getId(), "0.8", lastMod);
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String loc, String priority, String lastMod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastMod != null) {
            xml.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
        }
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
