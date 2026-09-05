package com.bob.ecommerceangularapp.bootstrap;

import com.bob.ecommerceangularapp.dao.CountryRepository;
import com.bob.ecommerceangularapp.dao.CouponRepository;
import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.FaqEntryRepository;
import com.bob.ecommerceangularapp.dao.GiftCardRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dao.ProductVariantRepository;
import com.bob.ecommerceangularapp.dao.ReviewRepository;
import com.bob.ecommerceangularapp.dao.ShippingMethodRepository;
import com.bob.ecommerceangularapp.dao.SiteBannerRepository;
import com.bob.ecommerceangularapp.dao.StateRepository;
import com.bob.ecommerceangularapp.dao.TaxRateRepository;
import com.bob.ecommerceangularapp.entity.Country;
import com.bob.ecommerceangularapp.entity.Coupon;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.FaqEntry;
import com.bob.ecommerceangularapp.entity.GiftCard;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import com.bob.ecommerceangularapp.entity.ProductVariant;
import com.bob.ecommerceangularapp.entity.Review;
import com.bob.ecommerceangularapp.entity.ShippingMethod;
import com.bob.ecommerceangularapp.entity.SiteBanner;
import com.bob.ecommerceangularapp.entity.State;
import com.bob.ecommerceangularapp.entity.TaxRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Idempotent seed data. Generates ~100 products across four categories plus reference geo data.
 * Only seeds an empty database, so it is safe to leave enabled with ddl-auto=update.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    /** Below this, a product counts as "low stock" — used to detect an already-varied DB. */
    private static final int LOW_STOCK_FLOOR = 6;

    /** Real, freely-licensed (Unsplash License) category photos, cycled across each category's products. */
    private static final Map<String, String[]> CATEGORY_IMAGES = Map.of(
            "Books", new String[]{
                    "https://images.unsplash.com/photo-1499447155021-4907f71b9ef5?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1695773719016-3d033a4aac07?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1662304729380-3a7ffb361e63?w=600&h=400&fit=crop&auto=format&q=80"},
            "Coffee Mugs", new String[]{
                    "https://images.unsplash.com/photo-1650959858546-d09833d5317b?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1635956541203-4625130f761f?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1702234844718-5f4f77a93baa?w=600&h=400&fit=crop&auto=format&q=80"},
            "Mouse Pads", new String[]{
                    "https://images.unsplash.com/photo-1702561667800-2c49b0182229?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1587749091716-f7b291a87f87?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1656071830624-06aa347f99a7?w=600&h=400&fit=crop&auto=format&q=80"},
            "Luggage", new String[]{
                    "https://images.unsplash.com/photo-1648737967037-96967e9151b5?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1639598003276-8a70fcaaad6c?w=600&h=400&fit=crop&auto=format&q=80",
                    "https://images.unsplash.com/photo-1502301197179-65228ab57f78?w=600&h=400&fit=crop&auto=format&q=80"});

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductVariantRepository variantRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CustomerRepository customerRepository;
    private final ReviewRepository reviewRepository;
    private final CouponRepository couponRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final TaxRateRepository taxRateRepository;
    private final GiftCardRepository giftCardRepository;
    private final SiteBannerRepository siteBannerRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final TransactionTemplate txTemplate;

    public DataLoader(ProductRepository productRepository,
                      ProductCategoryRepository productCategoryRepository,
                      ProductVariantRepository variantRepository,
                      CountryRepository countryRepository,
                      StateRepository stateRepository,
                      CustomerRepository customerRepository,
                      ReviewRepository reviewRepository,
                      CouponRepository couponRepository,
                      ShippingMethodRepository shippingMethodRepository,
                      TaxRateRepository taxRateRepository,
                      GiftCardRepository giftCardRepository,
                      SiteBannerRepository siteBannerRepository,
                      FaqEntryRepository faqEntryRepository,
                      PlatformTransactionManager transactionManager) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.variantRepository = variantRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.customerRepository = customerRepository;
        this.reviewRepository = reviewRepository;
        this.couponRepository = couponRepository;
        this.shippingMethodRepository = shippingMethodRepository;
        this.taxRateRepository = taxRateRepository;
        this.giftCardRepository = giftCardRepository;
        this.siteBannerRepository = siteBannerRepository;
        this.faqEntryRepository = faqEntryRepository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        seedCatalog();
        seedCountriesAndStates();
        backfillNewsletterDefaults();
        backfillSalePrices();
        backfillGalleryImages();
        backfillProductImages();
        backfillStockVariety();
        seedVariants();
        seedTaxAndShipping();
        seedGiftCards();
        seedReviews();
        seedCoupons();
        seedContent();
    }

    /**
     * Seeds SKU-level variants on the categories where they make sense (mugs/mouse-pads get sizes,
     * luggage gets colours); Books stay single-SKU to demonstrate a mixed catalog. Idempotent + safe
     * to run on an existing DB (it only seeds when there are products and no variants yet). Defensive.
     */
    private void seedVariants() {
        try {
            if (productRepository.count() == 0 || variantRepository.count() > 0) {
                return;
            }
            List<ProductVariant> variants = new ArrayList<>();
            for (Product p : productRepository.findAll()) {
                String category = p.getCategory() == null ? "" : p.getCategory().getCategoryName();
                switch (category) {
                    case "Coffee Mugs" -> {
                        variants.add(variant(p, "11OZ", null, "11oz", null, 0));
                        variants.add(variant(p, "15OZ", null, "15oz", plus(p, "2.00"), 1));
                    }
                    case "Mouse Pads" -> {
                        variants.add(variant(p, "M", null, "M", null, 0));
                        variants.add(variant(p, "L", null, "L", null, 1));
                        variants.add(variant(p, "XL", null, "XL", plus(p, "4.00"), 2));
                    }
                    case "Luggage" -> {
                        variants.add(variant(p, "BLK", "Black", null, null, 0));
                        variants.add(variant(p, "NVY", "Navy", null, null, 1));
                        variants.add(variant(p, "RED", "Red", null, null, 2));
                    }
                    default -> { /* Books: single-SKU */ }
                }
            }
            if (!variants.isEmpty()) {
                variantRepository.saveAll(variants);
                log.info("Seeded {} product variants.", variants.size());
            }
        } catch (Exception e) {
            log.warn("Skipped variant seeding (non-fatal): {}", e.getMessage());
        }
    }

    private ProductVariant variant(Product p, String suffix, String color, String size,
                                   BigDecimal priceOverride, int idx) {
        return ProductVariant.builder()
                .product(p)
                .sku(p.getSku() + "-" + suffix)
                .color(color)
                .size(size)
                .unitPrice(priceOverride)
                .unitsInStock(variantStock(p, idx))
                .sortOrder(idx)
                .active(true)
                .build();
    }

    private BigDecimal plus(Product p, String delta) {
        return p.getUnitPrice().add(new BigDecimal(delta));
    }

    /** Deterministic per-variant stock spread: most healthy, some low, the odd out-of-stock. */
    private int variantStock(Product p, int idx) {
        long id = p.getId() == null ? 0 : p.getId();
        int seed = (int) ((id * 7 + idx * 13) % 20);
        if (seed == 0) {
            return 0;            // out of stock
        }
        if (seed < 4) {
            return seed;         // low (1–3)
        }
        return 8 + seed;         // healthy
    }

    /**
     * Seeds shipping methods (Standard — free over $50 — and Express) and a handful of US state tax
     * rates, matched by the same display names the checkout sends. Idempotent: skips once any shipping
     * method exists. Defensive — never crashes the catalog. (Unlisted regions resolve to 0% tax.)
     */
    private void seedTaxAndShipping() {
        try {
            if (shippingMethodRepository.count() == 0) {
                shippingMethodRepository.saveAll(List.of(
                        shippingMethod("STANDARD", "Standard shipping", "5.99", "50.00", "3–5 business days", 0),
                        shippingMethod("EXPRESS", "Express shipping", "14.99", null, "1–2 business days", 1)));
                log.info("Seeded 2 shipping methods.");
            }
            if (taxRateRepository.count() == 0) {
                taxRateRepository.saveAll(List.of(
                        taxRate("United States", "California", "7.25"),
                        taxRate("United States", "New York", "8.88"),
                        taxRate("United States", "Texas", "6.25"),
                        taxRate("United States", "Washington", "6.50"),
                        taxRate("United States", "Florida", "6.00"),
                        taxRate("United States", "Illinois", "6.25"),
                        taxRate("United States", "Ohio", "5.75"),
                        taxRate("United States", "Pennsylvania", "6.00")));
                log.info("Seeded {} tax rates.", taxRateRepository.count());
            }
        } catch (Exception e) {
            log.warn("Skipped tax/shipping seeding (non-fatal): {}", e.getMessage());
        }
    }

    private ShippingMethod shippingMethod(String code, String name, String baseRate, String freeOver,
                                          String estimatedDays, int sortOrder) {
        return ShippingMethod.builder()
                .code(code)
                .name(name)
                .baseRate(new BigDecimal(baseRate))
                .freeOverThreshold(freeOver == null ? null : new BigDecimal(freeOver))
                .estimatedDays(estimatedDays)
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }

    private TaxRate taxRate(String country, String state, String ratePercent) {
        return TaxRate.builder()
                .country(country)
                .state(state)
                .ratePercent(new BigDecimal(ratePercent))
                .active(true)
                .build();
    }

    /** Seeds two demo gift cards (GIFT25 / GIFT50). Idempotent + defensive. */
    private void seedGiftCards() {
        try {
            if (giftCardRepository.count() > 0) {
                return;
            }
            giftCardRepository.saveAll(List.of(
                    giftCard("GIFT25", "25.00"),
                    giftCard("GIFT50", "50.00")));
            log.info("Seeded 2 gift cards (GIFT25, GIFT50).");
        } catch (Exception e) {
            log.warn("Skipped gift-card seeding (non-fatal): {}", e.getMessage());
        }
    }

    private GiftCard giftCard(String code, String balance) {
        BigDecimal amount = new BigDecimal(balance);
        return GiftCard.builder()
                .code(code)
                .initialBalance(amount)
                .balance(amount)
                .active(true)
                .build();
    }

    private void seedCoupons() {
        if (couponRepository.count() > 0) {
            return;
        }
        couponRepository.saveAll(List.of(
                coupon("WELCOME10", "10% off your first order", 10, null, null),
                coupon("SAVE5", "$5 off orders over $25", null, new BigDecimal("5.00"), new BigDecimal("25.00")),
                coupon("SUMMER20", "20% off — summer sale", 20, null, null)));
        log.info("Seeded 3 coupon codes.");
    }

    private Coupon coupon(String code, String description, Integer percentOff, BigDecimal amountOff, BigDecimal minSpend) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDescription(description);
        c.setPercentOff(percentOff);
        c.setAmountOff(amountOff);
        c.setMinSpend(minSpend);
        c.setActive(true);
        return c;
    }

    /**
     * Seeds the site banner + FAQ list with the content that used to be hardcoded in the frontend, so
     * a fresh DB looks the same as before this feature — from here on an admin owns both. Idempotent
     * + defensive, like the other reference-data seeds.
     */
    private void seedContent() {
        try {
            if (siteBannerRepository.count() == 0) {
                SiteBanner banner = new SiteBanner();
                banner.setMessage("This week's sale — up to 49% off · free shipping over $50.");
                banner.setLinkUrl("/sale");
                banner.setLinkText("Shop now");
                banner.setActive(true);
                siteBannerRepository.save(banner);
                log.info("Seeded 1 site banner.");
            }
            if (faqEntryRepository.count() == 0) {
                faqEntryRepository.saveAll(List.of(
                        faqEntry("How long does shipping take?",
                                "Standard shipping is 3–5 business days. Orders over $50 ship free; expedited options are shown at checkout.", 1),
                        faqEntry("What is your return policy?",
                                "Returns are accepted within 30 days of delivery, in original condition. See Shipping & Returns for the full details.", 2),
                        faqEntry("Which payment methods do you accept?",
                                "All major credit and debit cards via Stripe. Your card details are entered directly into Stripe and never touch our servers.", 3),
                        faqEntry("Do you ship internationally?",
                                "We ship to the United States, Canada, Brazil, Germany, India and Australia, with more regions on the way.", 4),
                        faqEntry("How do I track my order?",
                                "You'll get an order confirmation email with a tracking number, and you can view past orders under My account → My orders.", 5),
                        faqEntry("How do I manage marketing emails?",
                                "Update your preferences anytime in Account settings, or use the unsubscribe link at the bottom of any marketing email.", 6)));
                log.info("Seeded 6 FAQ entries.");
            }
        } catch (Exception e) {
            log.warn("Skipped content seeding (non-fatal): {}", e.getMessage());
        }
    }

    private FaqEntry faqEntry(String question, String answer, int sortOrder) {
        FaqEntry entry = new FaqEntry();
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setSortOrder(sortOrder);
        entry.setActive(true);
        return entry;
    }

    private static final String[] REVIEW_AUTHORS = {"Ada", "Linus", "Grace", "Alan", "Margaret",
            "Dennis", "Barbara", "Ken", "Katherine", "Tim", "Radia", "Guido"};
    private static final String[] REVIEW_COMMENTS = {
            "Exactly what I wanted — great quality.",
            "Solid value for the price. Would buy again.",
            "Arrived quickly and looks even better in person.",
            "Does the job well, no complaints.",
            "Really happy with this purchase!",
            "Good overall, though shipping took a couple extra days.",
            "Better than expected. Highly recommend.",
            "Decent product, met my expectations."};

    /** Seeds reviews on ~half the catalog (mostly 4–5★) and sets each product's rating aggregates. */
    private void seedReviews() {
        if (reviewRepository.count() > 0) {
            return;
        }
        List<Product> products = productRepository.findAll();
        List<Review> reviews = new ArrayList<>();
        List<Product> touched = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            if (i % 2 != 0) {
                continue; // about half the catalog gets reviews
            }
            Product product = products.get(i);
            int count = 1 + ((i * 7) % 6); // 1–6 reviews
            int sum = 0;
            for (int j = 0; j < count; j++) {
                int rating = ratingFor(i, j);
                Review review = new Review();
                review.setProductId(product.getId());
                review.setAuthorName(REVIEW_AUTHORS[(i + j) % REVIEW_AUTHORS.length]);
                review.setRating(rating);
                review.setComment(REVIEW_COMMENTS[(i * 3 + j) % REVIEW_COMMENTS.length]);
                review.setVerifiedBuyer((j % 2) == 0);
                reviews.add(review);
                sum += rating;
            }
            product.setAverageRating(Math.round((sum / (double) count) * 10.0) / 10.0);
            product.setReviewCount(count);
            touched.add(product);
        }

        reviewRepository.saveAll(reviews);
        productRepository.saveAll(touched);
        log.info("Seeded {} reviews across {} products.", reviews.size(), touched.size());
    }

    /** Deterministic, mostly-positive ratings (3–5★). */
    private int ratingFor(int i, int j) {
        int seed = (i + j) % 7;
        int rating = (seed == 0) ? 3 : (seed % 3 == 0) ? 4 : 5;
        return rating;
    }

    /**
     * Puts existing products on sale when none are yet (e.g. a DB seeded before the M6 sale feature),
     * so the /sale page has content without a full DB reset. Idempotent: once any product is on sale
     * this returns early. Defensive: never crashes the catalog.
     */
    private void backfillSalePrices() {
        try {
            if (productRepository.count() == 0
                    || productRepository.findByOriginalPriceNotNull(PageRequest.of(0, 1)).getTotalElements() > 0) {
                return;
            }
            List<Product> all = productRepository.findAll();
            List<Product> updated = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                Product p = all.get(i);
                if (p.getOriginalPrice() == null && i % 3 == 0) {
                    double markup = 1.20 + ((i * 7) % 30) / 100.0; // 20%–49% off
                    p.setOriginalPrice(p.getUnitPrice().multiply(BigDecimal.valueOf(markup)).setScale(2, RoundingMode.HALF_UP));
                    updated.add(p);
                }
            }
            if (!updated.isEmpty()) {
                productRepository.saveAll(updated);
                log.info("Backfilled sale prices on {} existing product(s).", updated.size());
            }
        } catch (Exception e) {
            log.warn("Skipped sale-price backfill (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Adds gallery images to products that have none (DBs seeded before the multi-image feature),
     * so the product page shows thumbnails without a full reset. Idempotent (skips products that
     * already have a gallery) and defensive: never crashes the catalog.
     */
    private void backfillGalleryImages() {
        try {
            // Run inside a transaction so the LAZY additionalImages collection can initialize
            // (the CommandLineRunner has no open session otherwise).
            Integer count = txTemplate.execute(status -> {
                int updated = 0;
                for (Product p : productRepository.findAll()) {
                    if ((p.getAdditionalImages() == null || p.getAdditionalImages().isEmpty())
                            && p.getCategory() != null) {
                        p.setAdditionalImages(galleryFor(p.getCategory()));
                        updated++;
                    }
                }
                return updated; // managed entities flush on commit
            });
            if (count != null && count > 0) {
                log.info("Backfilled gallery images on {} existing product(s).", count);
            }
        } catch (Exception e) {
            log.warn("Skipped gallery backfill (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Introduces a realistic stock spread (some low, the odd out-of-stock) on DBs seeded with
     * uniform stock, so the "Only N left"/out-of-stock UI has something to show. Idempotent:
     * returns early once any low/zero-stock product exists. Defensive: never crashes the catalog.
     */
    private void backfillStockVariety() {
        try {
            if (productRepository.count() == 0
                    || productRepository.countByUnitsInStockLessThan(LOW_STOCK_FLOOR) > 0) {
                return;
            }
            List<Product> all = productRepository.findAll();
            List<Product> updated = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                if (i % 17 == 5) {
                    all.get(i).setUnitsInStock(0);
                    updated.add(all.get(i));
                } else if (i % 11 == 3) {
                    all.get(i).setUnitsInStock(1 + (i % 4));
                    updated.add(all.get(i));
                }
            }
            if (!updated.isEmpty()) {
                productRepository.saveAll(updated);
                log.info("Backfilled stock variety on {} product(s).", updated.size());
            }
        } catch (Exception e) {
            log.warn("Skipped stock-variety backfill (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Replaces any lingering placehold.co URLs (main image or gallery) with real category photos, for
     * DBs seeded before real images existed. Idempotent: a product with no placehold.co reference left
     * is left untouched. Defensive: never crashes the catalog.
     */
    private void backfillProductImages() {
        try {
            Integer count = txTemplate.execute(status -> {
                int updated = 0;
                Map<String, Integer> seenPerCategory = new HashMap<>();
                for (Product p : productRepository.findAll()) {
                    boolean staleMain = p.getImageUrl() != null && p.getImageUrl().contains("placehold.co");
                    boolean staleGallery = p.getAdditionalImages() != null
                            && p.getAdditionalImages().stream().anyMatch(url -> url.contains("placehold.co"));
                    if ((!staleMain && !staleGallery) || p.getCategory() == null) {
                        continue;
                    }
                    String categoryName = p.getCategory().getCategoryName();
                    String[] images = CATEGORY_IMAGES.get(categoryName);
                    if (images == null) {
                        continue;
                    }
                    int idx = seenPerCategory.merge(categoryName, 1, Integer::sum) - 1;
                    p.setImageUrl(images[idx % images.length]);
                    p.setAdditionalImages(galleryFor(p.getCategory()));
                    updated++;
                }
                return updated; // managed entities flush on commit
            });
            if (count != null && count > 0) {
                log.info("Backfilled real photos on {} existing product(s).", count);
            }
        } catch (Exception e) {
            log.warn("Skipped product-image backfill (non-fatal): {}", e.getMessage());
        }
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
        String[] images = CATEGORY_IMAGES.get(category.getCategoryName());
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

            String sku = skuPrefix + "-" + String.format("%04d", i + 1);
            list.add(Product.builder()
                    .sku(sku)
                    .name(name)
                    .description(description)
                    .unitPrice(unitPrice)
                    .originalPrice(originalPrice)
                    .imageUrl(images[i % images.length])
                    .additionalImages(galleryFor(category))
                    .active(true)
                    .unitsInStock(stockFor(i))
                    .category(category)
                    .build());
        }
        return list;
    }

    /** A realistic stock spread: most healthy, some low ("Only N left"), the odd out-of-stock. */
    private int stockFor(int i) {
        if (i % 17 == 5) {
            return 0;                   // ~6% out of stock
        }
        if (i % 11 == 3) {
            return 1 + (i % 4);         // ~9% low stock (1–4 left)
        }
        return 15 + (i * 13) % 200;     // healthy
    }

    /** Extra gallery shots for the product page — the category's other real photos. */
    private List<String> galleryFor(ProductCategory category) {
        String[] images = CATEGORY_IMAGES.get(category.getCategoryName());
        return images == null ? new ArrayList<>() : new ArrayList<>(List.of(images));
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
