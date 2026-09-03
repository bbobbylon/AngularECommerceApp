package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for sitemap.xml generation. */
class SitemapServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductCategoryRepository categoryRepository = mock(ProductCategoryRepository.class);

    private SitemapService service(String frontendUrl) {
        return new SitemapService(productRepository, categoryRepository, frontendUrl);
    }

    private ProductCategory category(long id, String name) {
        ProductCategory category = new ProductCategory(name);
        category.setId(id);
        return category;
    }

    @Test
    void includesStaticPagesCategoriesAndActiveProducts() {
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Books")));
        when(productRepository.findByActiveTrue()).thenReturn(List.of(
                Product.builder().id(42L).name("Mug").lastUpdated(new Date()).build()));

        String xml = service("http://localhost:4250").buildSitemapXml();

        assertThat(xml).contains("<loc>http://localhost:4250/</loc>");
        assertThat(xml).contains("<loc>http://localhost:4250/products</loc>");
        assertThat(xml).contains("<loc>http://localhost:4250/category/1</loc>");
        assertThat(xml).contains("<loc>http://localhost:4250/products/42</loc>");
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    @Test
    void stripsTrailingSlashFromConfiguredFrontendUrl() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productRepository.findByActiveTrue()).thenReturn(List.of());

        String xml = service("http://localhost:4250/").buildSitemapXml();

        assertThat(xml).contains("<loc>http://localhost:4250/</loc>");
        assertThat(xml).doesNotContain("4250//");
    }

    @Test
    void omitsLastmodWhenProductHasNoTimestamp() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(productRepository.findByActiveTrue()).thenReturn(List.of(
                Product.builder().id(7L).name("No timestamp").lastUpdated(null).build()));

        String xml = service("http://localhost:4250").buildSitemapXml();

        assertThat(xml).contains("<loc>http://localhost:4250/products/7</loc>");
    }
}
