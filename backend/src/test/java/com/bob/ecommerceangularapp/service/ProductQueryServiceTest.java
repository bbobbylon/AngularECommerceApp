package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.ProductCardView;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for the faceted catalog search: drives the real {@code JpaSpecificationExecutor} against
 * H2 to prove each facet (active-only, category, keyword, price, in-stock, on-sale, rating) narrows
 * the results. The {@code @Cacheable} on {@code search} is inert here (the service is instantiated
 * directly, so there's no caching proxy) — exactly what we want for testing the query logic.
 */
@DataJpaTest
class ProductQueryServiceTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductCategoryRepository categoryRepository;

    private ProductQueryService service;
    private final Pageable page = PageRequest.of(0, 20);
    private ProductCategory books;
    private ProductCategory mugs;

    @BeforeEach
    void setUp() {
        service = new ProductQueryService(productRepository);
        books = categoryRepository.save(new ProductCategory("Books"));
        mugs = categoryRepository.save(new ProductCategory("Mugs"));
    }

    @Test
    void excludesInactiveProducts() {
        save("Active Book", books, p -> {});
        save("Hidden Book", books, p -> p.setActive(false));
        Page<ProductCardView> result = service.search(null, null, null, null, null, null, null, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("Active Book");
    }

    @Test
    void filtersByCategory() {
        save("Java Book", books, p -> {});
        save("Coffee Mug", mugs, p -> {});
        Page<ProductCardView> result = service.search(books.getId(), null, null, null, null, null, null, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("Java Book");
    }

    @Test
    void filtersByKeyword_caseInsensitive() {
        save("Spring in Action", books, p -> {});
        save("Cooking 101", books, p -> {});
        Page<ProductCardView> result = service.search(null, "spring", null, null, null, null, null, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("Spring in Action");
    }

    @Test
    void filtersByPriceRange() {
        save("Cheap", books, p -> p.setUnitPrice(new BigDecimal("5.00")));
        save("Mid", books, p -> p.setUnitPrice(new BigDecimal("25.00")));
        save("Pricey", books, p -> p.setUnitPrice(new BigDecimal("100.00")));
        Page<ProductCardView> result =
                service.search(null, null, new BigDecimal("10"), new BigDecimal("50"), null, null, null, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("Mid");
    }

    @Test
    void filtersInStockOnly() {
        save("In Stock", books, p -> p.setUnitsInStock(7));
        save("Sold Out", books, p -> p.setUnitsInStock(0));
        Page<ProductCardView> result = service.search(null, null, null, null, true, null, null, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("In Stock");
    }

    @Test
    void filtersOnSaleOnly() {
        save("On Sale", books, p -> p.setOriginalPrice(new BigDecimal("30.00")));
        save("Full Price", books, p -> {});
        Page<ProductCardView> result = service.search(null, null, null, null, null, true, null, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("On Sale");
    }

    @Test
    void filtersByMinimumRating() {
        save("Top Rated", books, p -> p.setAverageRating(4.6));
        save("Mediocre", books, p -> p.setAverageRating(2.0));
        save("Unrated", books, p -> {}); // null rating -> excluded by the >= predicate
        Page<ProductCardView> result = service.search(null, null, null, null, null, null, 4, page);
        assertThat(result.getContent()).extracting(ProductCardView::name).containsExactly("Top Rated");
    }

    private void save(String name, ProductCategory category, Consumer<Product> customizer) {
        Product p = Product.builder()
                .name(name).sku("SKU-" + name).description("desc")
                .unitPrice(new BigDecimal("19.99")).imageUrl("")
                .active(true).unitsInStock(5).category(category)
                .build();
        customizer.accept(p);
        productRepository.save(p);
    }
}
