package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import com.bob.ecommerceangularapp.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Newsletter subscription management + the weekly marketing blast.
 *
 * <p>Blast recipients = every customer with {@code newsletterSubscribed = true} ∪ every
 * standalone {@link NewsletterSubscriber}, deduplicated by email. Each recipient gets a
 * personal unsubscribe link backed by an opaque token.
 */
@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);
    private static final int FEATURED_COUNT = 4;

    private final NewsletterSubscriberRepository subscriberRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    public NewsletterService(NewsletterSubscriberRepository subscriberRepository,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository,
                             EmailService emailService) {
        this.subscriberRepository = subscriberRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
    }

    /** Subscribe via the signup box. Idempotent: re-subscribing reactivates an existing row. */
    @Transactional
    public void subscribe(String rawEmail, String name) {
        String email = normalize(rawEmail);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(email);
        if (subscriber == null) {
            subscriber = new NewsletterSubscriber();
            subscriber.setEmail(email);
        }
        if (name != null && !name.isBlank()) {
            subscriber.setName(name.trim());
        }
        subscriber.setSubscribed(true);
        subscriber.ensureUnsubscribeToken();
        subscriberRepository.save(subscriber);

        // Keep a matching customer's preference in sync.
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null && !customer.isNewsletterSubscribed()) {
            customer.setNewsletterSubscribed(true);
            customer.ensureUnsubscribeToken();
            customerRepository.save(customer);
        }

        String greetName = (name != null && !name.isBlank())
                ? name
                : (customer != null ? customer.getFirstName() : null);
        emailService.sendWelcome(email, greetName);
    }

    @Transactional
    public boolean unsubscribeByToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        boolean changed = false;

        NewsletterSubscriber subscriber = subscriberRepository.findByUnsubscribeToken(token);
        if (subscriber != null) {
            subscriber.setSubscribed(false);
            subscriberRepository.save(subscriber);
            changed = true;
        }
        Customer customer = customerRepository.findByUnsubscribeToken(token);
        if (customer != null) {
            customer.setNewsletterSubscribed(false);
            customerRepository.save(customer);
            changed = true;
        }
        return changed;
    }

    @Transactional
    public boolean unsubscribeByEmail(String rawEmail) {
        String email = normalize(rawEmail);
        if (email.isEmpty()) {
            return false;
        }
        boolean changed = false;

        NewsletterSubscriber subscriber = subscriberRepository.findByEmail(email);
        if (subscriber != null) {
            subscriber.setSubscribed(false);
            subscriberRepository.save(subscriber);
            changed = true;
        }
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            customer.setNewsletterSubscribed(false);
            customerRepository.save(customer);
            changed = true;
        }
        return changed;
    }

    /**
     * Sends the weekly edit to all subscribed recipients. No-op (returns 0) when email isn't
     * configured. Returns the number of recipients emailed.
     */
    @Transactional
    public int sendWeeklyBlast() {
        if (!emailService.isEnabled()) {
            log.info("Weekly newsletter blast skipped — email is not configured (set GMAIL_USERNAME/GMAIL_APP_PASSWORD).");
            return 0;
        }

        List<Product> featured = pickFeaturedProducts();
        if (featured.isEmpty()) {
            log.info("Weekly newsletter blast skipped — no products to feature.");
            return 0;
        }

        Map<String, Recipient> recipients = collectRecipients();
        for (Recipient recipient : recipients.values()) {
            emailService.sendWeeklyAd(recipient.email(), recipient.name(), featured, recipient.token());
        }
        log.info("Weekly newsletter blast sent to {} recipient(s).", recipients.size());
        return recipients.size();
    }

    /** Customers (subscribed) ∪ standalone subscribers, keyed by lowercased email (deduped). */
    private Map<String, Recipient> collectRecipients() {
        Map<String, Recipient> byEmail = new LinkedHashMap<>();

        for (Customer customer : customerRepository.findByNewsletterSubscribedTrue()) {
            String email = normalize(customer.getEmail());
            if (email.isEmpty()) {
                continue;
            }
            customer.ensureUnsubscribeToken();
            customerRepository.save(customer);
            byEmail.put(email, new Recipient(customer.getEmail(), customer.getFirstName(), customer.getUnsubscribeToken()));
        }

        for (NewsletterSubscriber subscriber : subscriberRepository.findBySubscribedTrue()) {
            String email = normalize(subscriber.getEmail());
            if (email.isEmpty() || byEmail.containsKey(email)) {
                continue;
            }
            subscriber.ensureUnsubscribeToken();
            subscriberRepository.save(subscriber);
            byEmail.put(email, new Recipient(subscriber.getEmail(), subscriber.getName(), subscriber.getUnsubscribeToken()));
        }

        return byEmail;
    }

    /** Prefer on-sale items for the blast; fall back to the newest catalog products. */
    private List<Product> pickFeaturedProducts() {
        List<Product> onSale = productRepository
                .findByOriginalPriceNotNull(PageRequest.of(0, FEATURED_COUNT))
                .getContent();
        if (!onSale.isEmpty()) {
            return onSale;
        }
        return productRepository.findAll(PageRequest.of(0, FEATURED_COUNT)).getContent();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record Recipient(String email, String name, String token) {
    }
}
