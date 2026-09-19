package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dto.AccountPreferences;
import com.bob.ecommerceangularapp.dto.AccountUpdateRequest;
import com.bob.ecommerceangularapp.dto.OrderHistoryView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account settings portal — lets a customer (or standalone subscriber) view and update their
 * email preferences, keyed by email. Identity is enforced on the frontend via the same dev/Okta
 * guard used for order history; this API trusts the supplied email (course-faithful simplicity).
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final CustomerRepository customerRepository;
    private final NewsletterSubscriberRepository subscriberRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    public AccountController(CustomerRepository customerRepository,
                            NewsletterSubscriberRepository subscriberRepository,
                            OrderRepository orderRepository,
                            EmailService emailService) {
        this.customerRepository = customerRepository;
        this.subscriberRepository = subscriberRepository;
        this.orderRepository = orderRepository;
        this.emailService = emailService;
    }

    /**
     * This tenant's order history for an email (My Orders page). Replaces the old raw Spring Data
     * REST search {@code /api/orders/search/findByCustomerEmailOrderByDateCreatedDesc}, which took no
     * tenant predicate at all — any authenticated caller (or anyone, when Okta isn't configured) could
     * read any tenant's customer's full order history for a known/guessed email (roadmap #21 gap).
     */
    @GetMapping("/orders")
    public PageResponse<OrderHistoryView> orders(@RequestParam String email,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        String normalized = normalize(email);
        return PageResponse.of(orderRepository
                .findByCustomerEmailAndTenantIdOrderByDateCreatedDesc(
                        normalized, TenantContext.currentTenantId(), PageRequest.of(page, size))
                .map(OrderHistoryView::of));
    }

    @GetMapping
    public ResponseEntity<AccountPreferences> getPreferences(@RequestParam String email) {
        String normalized = normalize(email);

        Customer customer = customerRepository.findByEmailAndTenantId(normalized, TenantContext.currentTenantId());
        if (customer != null) {
            return ResponseEntity.ok(new AccountPreferences(
                    customer.getFirstName(), customer.getLastName(), customer.getEmail(),
                    customer.isNewsletterSubscribed()));
        }

        NewsletterSubscriber subscriber = subscriberRepository.findByEmailAndTenantId(normalized, TenantContext.currentTenantId());
        if (subscriber != null) {
            String[] parts = splitName(subscriber.getName());
            return ResponseEntity.ok(new AccountPreferences(
                    parts[0], parts[1], subscriber.getEmail(), subscriber.isSubscribed()));
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping
    @Transactional
    public ResponseEntity<AccountPreferences> updatePreferences(@Valid @RequestBody AccountUpdateRequest request) {
        String normalized = normalize(request.email());
        AccountPreferences updated = null;

        Customer customer = customerRepository.findByEmailAndTenantId(normalized, TenantContext.currentTenantId());
        if (customer != null) {
            if (request.firstName() != null) {
                customer.setFirstName(request.firstName().trim());
            }
            if (request.lastName() != null) {
                customer.setLastName(request.lastName().trim());
            }
            if (request.newsletterSubscribed() != null) {
                customer.setNewsletterSubscribed(request.newsletterSubscribed());
            }
            customer.ensureUnsubscribeToken();
            customerRepository.save(customer);
            syncSubscriber(normalized, customer.isNewsletterSubscribed());
            updated = new AccountPreferences(customer.getFirstName(), customer.getLastName(),
                    customer.getEmail(), customer.isNewsletterSubscribed());
        } else {
            NewsletterSubscriber subscriber = subscriberRepository.findByEmailAndTenantId(normalized, TenantContext.currentTenantId());
            if (subscriber != null) {
                if (request.newsletterSubscribed() != null) {
                    subscriber.setSubscribed(request.newsletterSubscribed());
                }
                subscriber.ensureUnsubscribeToken();
                subscriberRepository.save(subscriber);
                String[] parts = splitName(subscriber.getName());
                updated = new AccountPreferences(parts[0], parts[1], subscriber.getEmail(), subscriber.isSubscribed());
            }
        }

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        emailService.sendSettingsUpdated(updated.email(),
                (updated.firstName() == null ? "" : updated.firstName()), updated.newsletterSubscribed());
        return ResponseEntity.ok(updated);
    }

    /** Keep a standalone subscriber row (if any) in sync with the customer's preference. */
    private void syncSubscriber(String email, boolean subscribed) {
        NewsletterSubscriber subscriber = subscriberRepository.findByEmailAndTenantId(email, TenantContext.currentTenantId());
        if (subscriber != null && subscriber.isSubscribed() != subscribed) {
            subscriber.setSubscribed(subscribed);
            subscriberRepository.save(subscriber);
        }
    }

    private static String[] splitName(String name) {
        if (name == null || name.isBlank()) {
            return new String[]{"", ""};
        }
        String[] tokens = name.trim().split("\\s+", 2);
        return new String[]{tokens[0], tokens.length > 1 ? tokens[1] : ""};
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
