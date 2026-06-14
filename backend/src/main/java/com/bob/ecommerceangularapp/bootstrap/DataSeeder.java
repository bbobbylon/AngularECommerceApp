package com.bob.ecommerceangularapp.bootstrap;

import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Idempotent seed data so the catalog has something to show before the course
 * SQL scripts are swapped in. Runs once on startup when the product table is empty.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedCatalog(ProductCategoryRepository categoryRepository,
                                  ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            // category name -> list of product names
            Map<String, List<String>> catalog = new LinkedHashMap<>();
            catalog.put("Books", List.of(
                    "JavaScript - The Fun Parts", "The Power of Reactive Programming",
                    "Spring Boot in Action", "Mastering Angular Signals",
                    "Clean Code Cookbook", "Effective Java for Everyone"));
            catalog.put("Coffee Mugs", List.of(
                    "Luv2Code Coffee Mug", "Stay Caffeinated Mug",
                    "Code & Coffee Mug", "Debugging Fuel Mug", "Hello World Mug"));
            catalog.put("Mouse Pads", List.of(
                    "Luv2Code Mouse Pad", "Ergonomic Gel Mouse Pad",
                    "Extended Desk Mouse Pad", "Retro Grid Mouse Pad", "Minimalist Mouse Pad"));
            catalog.put("Luggage", List.of(
                    "Carry-On Spinner", "Weekend Duffel Bag",
                    "Hardshell Checked Bag", "Laptop Backpack", "Travel Toiletry Kit"));

            int sku = 1000;
            int priceSeed = 0;
            List<ProductCategory> categories = new ArrayList<>();
            List<Product> products = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : catalog.entrySet()) {
                ProductCategory category = new ProductCategory();
                category.setCategoryName(entry.getKey());
                categories.add(category);

                for (String productName : entry.getValue()) {
                    Product product = new Product();
                    product.setSku("PROD-" + (sku++));
                    product.setName(productName);
                    product.setDescription("Sample product: " + productName);
                    product.setUnitPrice(BigDecimal.valueOf(9.99 + (priceSeed++ * 2.5)));
                    product.setImageUrl("https://placehold.co/300x300?text=" +
                            productName.replace(" ", "+"));
                    product.setActive(true);
                    product.setUnitsInStock(100);
                    product.setCategory(category);
                    products.add(product);
                }
            }

            categoryRepository.saveAll(categories);
            productRepository.saveAll(products);

            System.out.println("Seeded " + categories.size() + " categories and "
                    + products.size() + " products.");
        };
    }
}
