package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.StockNotificationRepository;
import com.bob.ecommerceangularapp.dto.StockNotificationRequest;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.StockNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "Notify me when back in stock". Customers subscribe to an out-of-stock product (or a specific
 * variant); when an admin restocks it, every waiting subscriber is emailed once (the email itself is
 * gated by {@link EmailService}, so this is a safe no-op without SMTP). Subscriptions are deduped per
 * email + product/variant while still waiting.
 */
@Service
public class StockNotificationService {

    private static final Logger log = LoggerFactory.getLogger(StockNotificationService.class);

    private final StockNotificationRepository repository;
    private final EmailService emailService;

    public StockNotificationService(StockNotificationRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional
    public void subscribe(StockNotificationRequest request) {
        String sku = (request.variantSku() == null || request.variantSku().isBlank())
                ? null : request.variantSku().trim();
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCaseAndProductIdAndVariantSkuAndNotifiedFalse(
                email, request.productId(), sku)) {
            return; // already waiting
        }
        StockNotification n = new StockNotification();
        n.setEmail(email);
        n.setProductId(request.productId());
        n.setVariantSku(sku);
        n.setNotified(false);
        repository.save(n);
    }

    /** Emails everyone waiting on the product itself (not a specific variant). Called on admin restock. */
    @Transactional
    public void notifyProductRestocked(Product product) {
        if (product == null || product.getUnitsInStock() <= 0) {
            return;
        }
        notifyAll(repository.findByProductIdAndVariantSkuIsNullAndNotifiedFalse(product.getId()), product);
    }

    /** Emails everyone waiting on a specific variant. Called when a restocked variant is saved. */
    @Transactional
    public void notifyVariantRestocked(String variantSku, Product product) {
        if (variantSku == null || variantSku.isBlank() || product == null) {
            return;
        }
        notifyAll(repository.findByVariantSkuAndNotifiedFalse(variantSku), product);
    }

    private void notifyAll(List<StockNotification> waiting, Product product) {
        if (waiting.isEmpty()) {
            return;
        }
        for (StockNotification n : waiting) {
            emailService.sendBackInStock(n.getEmail(), product.getName(), product.getId());
            n.setNotified(true);
        }
        repository.saveAll(waiting);
        log.info("Notified {} subscriber(s) that '{}' is back in stock.", waiting.size(), product.getName());
    }
}
