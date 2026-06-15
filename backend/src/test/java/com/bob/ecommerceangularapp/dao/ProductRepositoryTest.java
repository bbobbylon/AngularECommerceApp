package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for the Spring Data JPA query methods that back the catalog endpoints,
 * running against the in-memory H2 database.
 */
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Test
    void findByCategoryId_returnsOnlyProductsInThatCategory() {
        ProductCategory books = categoryRepository.save(new ProductCategory("Books"));
        ProductCategory mugs = categoryRepository.save(new ProductCategory("Mugs"));
        saveProduct("Java Book", books);
        saveProduct("Spring Book", books);
        saveProduct("Coffee Mug", mugs);

        Page<Product> page = productRepository.findByCategoryId(books.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Java Book", "Spring Book");
    }

    @Test
    void findByNameContaining_matchesSubstring() {
        ProductCategory books = categoryRepository.save(new ProductCategory("Books"));
        saveProduct("Spring in Action", books);
        saveProduct("Angular in Action", books);
        saveProduct("Cooking 101", books);

        Page<Product> page = productRepository.findByNameContaining("Action", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Spring in Action", "Angular in Action");
    }

    @Test
    void findByNameContaining_paginatesResults() {
        ProductCategory books = categoryRepository.save(new ProductCategory("Books"));
        for (int i = 0; i < 5; i++) {
            saveProduct("Guide " + i, books);
        }

        Page<Product> firstPage = productRepository.findByNameContaining("Guide", PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    private void saveProduct(String name, ProductCategory category) {
        Product product = Product.builder()
                .name(name)
                .sku("SKU-" + name)
                .description("desc")
                .unitPrice(new BigDecimal("9.99"))
                .imageUrl("")
                .active(true)
                .unitsInStock(5)
                .category(category)
                .build();
        productRepository.save(product);
    }
}
