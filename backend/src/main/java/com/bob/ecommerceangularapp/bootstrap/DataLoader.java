package com.bob.ecommerceangularapp.bootstrap;

import com.bob.ecommerceangularapp.dao.CountryRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.StateRepository;
import com.bob.ecommerceangularapp.entity.Country;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.entity.State;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Idempotent seed data for the product catalog. Runs once on startup and only inserts
 * when the product table is empty, so it is safe to leave enabled with ddl-auto=update.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;

    public DataLoader(ProductRepository productRepository,
                      ProductCategoryRepository productCategoryRepository,
                      CountryRepository countryRepository,
                      StateRepository stateRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
    }

    @Override
    public void run(String... args) {
        seedCatalog();
        seedCountriesAndStates();
    }

    private void seedCatalog() {
        if (productRepository.count() > 0) {
            return;
        }

        ProductCategory books = new ProductCategory("Books");
        ProductCategory coffeeMugs = new ProductCategory("Coffee Mugs");
        ProductCategory mousePads = new ProductCategory("Mouse Pads");
        ProductCategory luggage = new ProductCategory("Luggage");
        productCategoryRepository.saveAll(List.of(books, coffeeMugs, mousePads, luggage));

        List<Product> products = new ArrayList<>();

        // Books
        products.add(product("BOOK-TECH-1000", "The Art of Spring Boot", books,
                "A hands-on guide to building production-ready Spring Boot applications.",
                "21.99", 100));
        products.add(product("BOOK-TECH-1001", "Angular in Action", books,
                "Master modern standalone Angular with practical, real-world examples.",
                "27.99", 100));
        products.add(product("BOOK-TECH-1002", "Full Stack Foundations", books,
                "Connect a Java backend to an Angular frontend the right way.",
                "24.49", 100));
        products.add(product("BOOK-TECH-1003", "Clean Java", books,
                "Write readable, maintainable Java with confidence.",
                "19.99", 100));
        products.add(product("BOOK-TECH-1004", "SQL for Developers", books,
                "Everything a backend developer needs to know about relational databases.",
                "18.50", 100));

        // Coffee Mugs
        products.add(product("MUG-COFFEE-2000", "Code & Coffee Mug", coffeeMugs,
                "11oz ceramic mug for the caffeine-driven developer.",
                "12.99", 200));
        products.add(product("MUG-COFFEE-2001", "Null Pointer Mug", coffeeMugs,
                "A mug that's never empty (unless it is).",
                "13.49", 200));
        products.add(product("MUG-COFFEE-2002", "Ship It Mug", coffeeMugs,
                "Motivation in ceramic form.",
                "11.99", 200));
        products.add(product("MUG-COFFEE-2003", "Stay Caffeinated Mug", coffeeMugs,
                "Extra-large 15oz mug for long debugging sessions.",
                "14.99", 200));
        products.add(product("MUG-COFFEE-2004", "Hello World Mug", coffeeMugs,
                "The first mug every programmer should own.",
                "10.99", 200));

        // Mouse Pads
        products.add(product("PAD-MOUSE-3000", "Pro Gaming Mouse Pad", mousePads,
                "Large low-friction surface for precise tracking.",
                "16.99", 150));
        products.add(product("PAD-MOUSE-3001", "Ergonomic Wrist Pad", mousePads,
                "Memory-foam wrist support for all-day comfort.",
                "18.99", 150));
        products.add(product("PAD-MOUSE-3002", "Desk Mat XL", mousePads,
                "Full-desk mat that doubles as a mouse pad.",
                "29.99", 150));
        products.add(product("PAD-MOUSE-3003", "Minimal Mouse Pad", mousePads,
                "Slim, water-resistant, and durable.",
                "9.99", 150));
        products.add(product("PAD-MOUSE-3004", "RGB Mouse Pad", mousePads,
                "Light up your desk with customizable RGB edges.",
                "24.99", 150));

        // Luggage
        products.add(product("LUGG-TRAVEL-4000", "Carry-On Spinner", luggage,
                "Lightweight hard-shell carry-on with 360-degree wheels.",
                "89.99", 75));
        products.add(product("LUGG-TRAVEL-4001", "Checked Suitcase", luggage,
                "Spacious checked bag with an expandable zipper.",
                "129.99", 75));
        products.add(product("LUGG-TRAVEL-4002", "Weekender Duffel", luggage,
                "Durable canvas duffel sized for a long weekend.",
                "59.99", 75));
        products.add(product("LUGG-TRAVEL-4003", "Laptop Backpack", luggage,
                "Padded travel backpack with a dedicated laptop sleeve.",
                "64.99", 75));
        products.add(product("LUGG-TRAVEL-4004", "Packing Cube Set", luggage,
                "Stay organized with a set of four packing cubes.",
                "24.99", 75));

        productRepository.saveAll(products);
    }

    private void seedCountriesAndStates() {
        if (countryRepository.count() > 0) {
            return;
        }

        Country usa = new Country("US", "United States");
        Country canada = new Country("CA", "Canada");
        Country brazil = new Country("BR", "Brazil");
        Country germany = new Country("DE", "Germany");
        Country india = new Country("IN", "India");
        Country australia = new Country("AU", "Australia");
        countryRepository.saveAll(List.of(usa, canada, brazil, germany, india, australia));

        List<State> states = new ArrayList<>();
        addStates(states, usa, "California", "Texas", "New York", "Florida",
                "Pennsylvania", "Illinois", "Ohio", "Washington");
        addStates(states, canada, "Ontario", "Quebec", "British Columbia",
                "Alberta", "Manitoba", "Nova Scotia");
        addStates(states, brazil, "Sao Paulo", "Rio de Janeiro", "Bahia", "Parana");
        addStates(states, germany, "Bavaria", "Berlin", "Hamburg", "Hesse", "Saxony");
        addStates(states, india, "Maharashtra", "Karnataka", "Tamil Nadu",
                "Delhi", "Gujarat", "West Bengal");
        addStates(states, australia, "New South Wales", "Victoria", "Queensland",
                "Western Australia", "Tasmania");
        stateRepository.saveAll(states);
    }

    private void addStates(List<State> target, Country country, String... names) {
        for (String name : names) {
            target.add(new State(name, country));
        }
    }

    private Product product(String sku, String name, ProductCategory category,
                            String description, String unitPrice, int unitsInStock) {
        String label = name.replace(" ", "+");
        return Product.builder()
                .sku(sku)
                .name(name)
                .description(description)
                .unitPrice(new BigDecimal(unitPrice))
                .imageUrl("https://placehold.co/600x400?text=" + label)
                .active(true)
                .unitsInStock(unitsInStock)
                .category(category)
                .build();
    }
}
