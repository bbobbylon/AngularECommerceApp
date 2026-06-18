package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductCategoryRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.AdminOrderView;
import com.bob.ecommerceangularapp.dto.AdminProductRequest;
import com.bob.ecommerceangularapp.dto.AdminStats;
import com.bob.ecommerceangularapp.config.CacheConfig;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.ProductCategory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Back-office operations: dashboard metrics, product CRUD, order management, categories. */
@Service
public class AdminService {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final String IMG_FALLBACK = "https://placehold.co/600x400/eef2fb/8b93ab?text=";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final NewsletterSubscriberRepository subscriberRepository;
    private final StockNotificationService stockNotificationService;

    public AdminService(ProductRepository productRepository,
                        ProductCategoryRepository productCategoryRepository,
                        OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        NewsletterSubscriberRepository subscriberRepository,
                        StockNotificationService stockNotificationService) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.subscriberRepository = subscriberRepository;
        this.stockNotificationService = stockNotificationService;
    }

    public AdminStats stats() {
        long subscribers = customerRepository.countByNewsletterSubscribedTrue()
                + subscriberRepository.countBySubscribedTrue();
        return new AdminStats(
                productRepository.count(),
                productRepository.countByActiveTrue(),
                productRepository.countByUnitsInStockLessThan(LOW_STOCK_THRESHOLD),
                productRepository.countByOriginalPriceNotNull(),
                orderRepository.count(),
                orderRepository.sumTotalRevenue(),
                customerRepository.count(),
                subscribers);
    }

    // ----- products -----

    public Page<Product> listProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CATALOG_SEARCH, allEntries = true)
    public Product createProduct(AdminProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CATALOG_SEARCH, allEntries = true)
    public Product updateProduct(Long id, AdminProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        apply(product, request);
        Product saved = productRepository.save(product);
        // If this update brings the product back in stock, email anyone waiting (gated).
        if (saved.getUnitsInStock() > 0) {
            stockNotificationService.notifyProductRestocked(saved);
        }
        return saved;
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CATALOG_SEARCH, allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    private void apply(Product product, AdminProductRequest request) {
        ProductCategory category = productCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice());
        product.setOriginalPrice(normalizeSalePrice(request.originalPrice(), request.unitPrice()));
        product.setImageUrl(imageOrFallback(request.imageUrl(), request.name()));
        product.setAdditionalImages(cleanImages(request.additionalImages()));
        product.setActive(request.active());
        product.setUnitsInStock(request.unitsInStock());
        product.setCategory(category);
    }

    /** Only keep an "original" (was) price when it's genuinely higher than the current price. */
    private BigDecimal normalizeSalePrice(BigDecimal original, BigDecimal unitPrice) {
        if (original == null || unitPrice == null || original.compareTo(unitPrice) <= 0) {
            return null;
        }
        return original;
    }

    private String imageOrFallback(String imageUrl, String name) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.trim();
        }
        String label = (name == null || name.isBlank()) ? "Product" : name.trim();
        return IMG_FALLBACK + URLEncoder.encode(label, StandardCharsets.UTF_8);
    }

    /** Drops null/blank gallery URLs and trims the rest; never returns null. */
    private List<String> cleanImages(List<String> images) {
        if (images == null) {
            return new ArrayList<>();
        }
        return images.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    // ----- orders -----

    public Page<AdminOrderView> listOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByDateCreatedDesc(pageable).map(this::toOrderView);
    }

    @Transactional
    public AdminOrderView updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setStatus(status);
        return toOrderView(orderRepository.save(order));
    }

    private AdminOrderView toOrderView(Order order) {
        Customer customer = order.getCustomer();
        String name = customer == null ? "—"
                : ((safe(customer.getFirstName()) + " " + safe(customer.getLastName())).trim());
        String email = customer == null ? "—" : customer.getEmail();
        return new AdminOrderView(order.getId(), order.getOrderTrackingNumber(), order.getStatus(),
                order.getTotalQuantity(), order.getTotalPrice(), order.getDateCreated(),
                name.isBlank() ? "—" : name, email);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // ----- categories -----

    @Transactional
    public ProductCategory createCategory(String name) {
        return productCategoryRepository.save(new ProductCategory(name.trim()));
    }
}
