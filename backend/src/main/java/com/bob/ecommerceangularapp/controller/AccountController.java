package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dto.AccountPreferences;
import com.bob.ecommerceangularapp.dto.AccountUpdateRequest;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final CustomerRepository customerRepository;
    private final NewsletterSubscriberRepository subscriberRepository;
    private final EmailService emailService;

    public AccountController(CustomerRepository customerRepository,
                            NewsletterSubscriberRepository subscriberRepository,
                            EmailService emailService) {
        this.customerRepository = customerRepository;
        this.subscriberRepository = subscriberRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<AccountPreferences> getPreferences(@RequestParam String email) {
        String normalized = normalize(email);

        Customer customer = customerRepository.findByEmail(normalized);
        if (customer != null) {
            return ResponseEntity.ok(new AccountPreferences(
                    customer.getFirstName(), customer.getLastName(), customer.getEmail(),
                    customer.isNewsletterSubscribed()));
        }

        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(normalized);
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

        Customer customer = customerRepository.findByEmail(normalized);
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
            NewsletterSubscriber subscriber = subscriberRepository.findByEmail(normalized);
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
        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(email);
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
