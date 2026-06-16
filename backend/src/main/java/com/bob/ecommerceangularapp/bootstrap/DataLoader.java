package com.bob.ecommerceangularapp.bootstrap;

import com.bob.ecommerceangularapp.dao.CountryRepository;
import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.StateRepository;
import com.bob.ecommerceangularapp.entity.Country;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.entity.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Idempotent seed data. Generates ~100 products across four categories plus reference geo data.
 * Only seeds an empty database, so it is safe to leave enabled with ddl-auto=update.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final String IMG = "https://placehold.co/600x400/eef2fb/8b93ab?text=";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CustomerRepository customerRepository;

    public DataLoader(ProductRepository productRepository,
                      ProductCategoryRepository productCategoryRepository,
                      CountryRepository countryRepository,
                      StateRepository stateRepository,
                      CustomerRepository customerRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) {
        seedCatalog();
        seedCountriesAndStates();
        backfillNewsletterDefaults();
    }

    /**
     * One-time backfill so "all users in the database" receive the weekly email (M6 intent).
     * Only touches customers without an unsubscribe token — i.e. rows created before email existed.
     * Customers created at checkout always get a token (even opt-outs), so this never re-subscribes
     * someone who deliberately opted out.
     *
     * <p>Wrapped defensively: this is an auxiliary step, so a hiccup here (e.g. schema drift on a
     * populated table) is logged and swallowed — it must never take down the catalog API.
     */
    private void backfillNewsletterDefaults() {
        try {
            List<Customer> pending = customerRepository.findAll().stream()
                    .filter(c -> c.getUnsubscribeToken() == null || c.getUnsubscribeToken().isBlank())
                    .toList();
            if (pending.isEmpty()) {
                return;
            }
            for (Customer customer : pending) {
                customer.setNewsletterSubscribed(true);
                customer.ensureUnsubscribeToken();
            }
            customerRepository.saveAll(pending);
            log.info("Backfilled newsletter defaults for {} existing customer(s).", pending.size());
        } catch (Exception e) {
            log.warn("Skipped newsletter backfill (non-fatal): {}", e.getMessage());
        }
    }

    private void seedCatalog() {
        if (productRepository.count() > 0) {
            return;
        }

        ProductCategory books = new ProductCategory("Books");
        ProductCategory mugs = new ProductCategory("Coffee Mugs");
        ProductCategory mousePads = new ProductCategory("Mouse Pads");
        ProductCategory luggage = new ProductCategory("Luggage");
        productCategoryRepository.saveAll(List.of(books, mugs, mousePads, luggage));

        List<Product> products = new ArrayList<>();

        products.addAll(generate(books, "BOOK",
                new String[]{"Java", "Spring Boot", "Angular", "TypeScript", "SQL", "Docker",
                        "Kubernetes", "Python", "Go", "Rust", "React", "Microservices", "DevOps",
                        "Algorithms", "System Design", "Clean Code", "REST APIs", "GraphQL",
                        "Linux", "Git"},
                new String[]{"in Action", "Handbook", "Cookbook", "Deep Dive", "Field Guide", "Essentials"},
                "A practical, example-driven guide that takes you from the fundamentals to real-world mastery.",
                14.99, 49.99, 25));

        products.addAll(generate(mugs, "MUG",
                new String[]{"Code & Coffee", "Null Pointer", "Ship It", "Stay Caffeinated", "Hello World",
                        "Rubber Duck", "Compile & Sip", "Brew Loop", "Deploy Friday", "Stack Overflow",
                        "Merge Conflict", "Hotfix", "Caffeine Driven", "Dark Mode", "Semicolon"},
                new String[]{"Mug", "Ceramic Mug", "Travel Mug", "XL Mug"},
                "An 11oz ceramic mug for the caffeine-driven developer. Dishwasher and microwave safe.",
                9.99, 18.99, 25));

        products.addAll(generate(mousePads, "PAD",
                new String[]{"Pro Gaming", "Ergonomic", "Desk Mat XL", "Minimal", "RGB", "Cloth",
                        "Hard Surface", "Wrist Rest", "Extended", "Compact", "Glass", "Eco Cork",
                        "Speed Edition", "Control Edition", "Studio"},
                new String[]{"Mouse Pad", "Pad", "Desk Pad", "Mat"},
                "A low-friction surface for precise tracking, with a non-slip rubber base and stitched edges.",
                8.99, 29.99, 25));

        products.addAll(generate(luggage, "LUGG",
                new String[]{"Carry-On Spinner", "Checked Suitcase", "Weekender Duffel", "Laptop Backpack",
                        "Packing Cube Set", "Hard-Shell Case", "Garment Bag", "Travel Tote", "Cabin Bag",
                        "Expandable Trolley", "Anti-Theft Pack", "Rolling Holdall", "Toiletry Kit",
                        "Tech Organizer", "Travel Sling"},
                new String[]{"", "Pro", "Lite", "Deluxe"},
                "Durable, lightweight travel gear built for the desk, the commute, and everywhere in between.",
                24.99, 149.99, 25));

        productRepository.saveAll(products);
    }

    /** Builds {@code count} products for a category from combinations of name parts. */
    private List<Product> generate(ProductCategory category, String skuPrefix,
                                   String[] partsA, String[] partsB, String description,
                                   double minPrice, double maxPrice, int count) {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = (partsA[i % partsA.length] + " " + partsB[i % partsB.length]).trim();
            double price = minPrice + ((maxPrice - minPrice) * ((i * 37) % 100) / 100.0);
            BigDecimal unitPrice = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);

            // Put roughly every third item on sale: unitPrice is the deal, originalPrice the "was".
            BigDecimal originalPrice = null;
            if (i % 3 == 0) {
                double markup = 1.20 + ((i * 7) % 30) / 100.0; // 20%–49% off
                originalPrice = unitPrice.multiply(BigDecimal.valueOf(markup)).setScale(2, RoundingMode.HALF_UP);
            }

            int stock = 15 + (i * 13) % 200;
            String sku = skuPrefix + "-" + String.format("%04d", i + 1);
            list.add(Product.builder()
                    .sku(sku)
                    .name(name)
                    .description(description)
                    .unitPrice(unitPrice)
                    .originalPrice(originalPrice)
                    .imageUrl(IMG + category.getCategoryName().replace(" ", "+"))
                    .active(true)
                    .unitsInStock(stock)
                    .category(category)
                    .build());
        }
        return list;
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
}
