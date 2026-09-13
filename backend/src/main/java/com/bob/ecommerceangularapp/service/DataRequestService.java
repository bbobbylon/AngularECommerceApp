package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.AbandonedCartRepository;
import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.DataRequestRepository;
import com.bob.ecommerceangularapp.dao.GiftCardRepository;
import com.bob.ecommerceangularapp.dao.LoyaltyTransactionRepository;
import com.bob.ecommerceangularapp.dao.NewsletterSubscriberRepository;
import com.bob.ecommerceangularapp.dao.ReferralRepository;
import com.bob.ecommerceangularapp.dao.ReturnRequestRepository;
import com.bob.ecommerceangularapp.dao.SavedAddressRepository;
import com.bob.ecommerceangularapp.dao.SavedPaymentMethodRepository;
import com.bob.ecommerceangularapp.dao.StockNotificationRepository;
import com.bob.ecommerceangularapp.dao.WishlistItemRepository;
import com.bob.ecommerceangularapp.dto.DataExportView;
import com.bob.ecommerceangularapp.dto.DataRequestSubmission;
import com.bob.ecommerceangularapp.dto.DataRequestView;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.AbandonedCart;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.DataRequest;
import com.bob.ecommerceangularapp.entity.GiftCard;
import com.bob.ecommerceangularapp.entity.LoyaltyTransaction;
import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Referral;
import com.bob.ecommerceangularapp.entity.ReturnRequest;
import com.bob.ecommerceangularapp.entity.SavedAddress;
import com.bob.ecommerceangularapp.entity.SavedPaymentMethod;
import com.bob.ecommerceangularapp.entity.StockNotification;
import com.bob.ecommerceangularapp.entity.WishlistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Self-service data-subject rights (roadmap #24): "send me everything you hold" (GDPR Art. 15/20)
 * and "erase me" (Art. 17).
 *
 * <h2>Why a mailed token</h2>
 * {@code /api/account/**} trusts whatever email it is handed — fine for reading newsletter
 * preferences, catastrophic for these two operations, where it would let anyone export a stranger's
 * order history or wipe their account by typing their address. So a request does nothing until it
 * is confirmed from a link sent to that mailbox, reusing the idiom {@code Customer.unsubscribeToken}
 * established in M6. This works with or without Okta, which matters because the storefront runs
 * fully open in local/demo mode.
 *
 * <h2>Why erasure anonymizes instead of deleting</h2>
 * {@code Customer.orders} is mapped {@code CascadeType.ALL}, so deleting the customer row would take
 * the subject's entire order history with it — the financial records tax and accounting law require
 * retaining. Art. 17(3)(b)/(e) already carves those out, so erasure tombstones the customer's
 * identifying fields, deletes the marketing and convenience rows outright, and leaves the orders
 * standing with their contact linkage cut. Street lines are cleared from order addresses while
 * city/state/country/postcode stay: the retention basis is proving the <em>tax jurisdiction</em> of a
 * sale, which does not require the doorstep.
 *
 * <h2>What is out of scope, and why it is said out loud</h2>
 * Product reviews store {@code authorName} only — no email, no customer FK — so there is no
 * non-guessing way to match a review to a data subject. They are pseudonymous by construction rather
 * than overlooked, and both the export bundle and the erasure summary say so explicitly, because a
 * silent omission is indistinguishable from a bug.
 */
@Service
public class DataRequestService {

    private static final Logger log = LoggerFactory.getLogger(DataRequestService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Tombstone domain. {@code .invalid} is reserved by RFC 2606 — it can never route anywhere. */
    private static final String ERASED_DOMAIN = "@erased.invalid";
    private static final String ERASED_NAME = "Erased";

    private static final String REVIEWS_NOTICE =
            "Product reviews are stored against a display name only, with no email address or account "
                    + "link, so they cannot be matched to you and are neither exported nor erased here. "
                    + "Contact us with the review text if you would like one removed.";

    private static final String EXPORT_NOTICE =
            "Everything Luv2Shop holds that is linked to this email address. Amounts are in USD. "
                    + "Saved cards list a payment-processor reference, a brand and the last four digits "
                    + "only — no card number was ever stored.";

    private final DataRequestRepository dataRequestRepository;
    private final CustomerRepository customerRepository;
    private final SavedAddressRepository savedAddressRepository;
    private final SavedPaymentMethodRepository savedPaymentMethodRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final StockNotificationRepository stockNotificationRepository;
    private final AbandonedCartRepository abandonedCartRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final ReferralRepository referralRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final GiftCardRepository giftCardRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final PrivacyConsentService privacyConsentService;
    private final PaymentMethodService paymentMethodService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final String apiUrl;
    private final int ttlHours;

    public DataRequestService(DataRequestRepository dataRequestRepository,
                              CustomerRepository customerRepository,
                              SavedAddressRepository savedAddressRepository,
                              SavedPaymentMethodRepository savedPaymentMethodRepository,
                              WishlistItemRepository wishlistItemRepository,
                              StockNotificationRepository stockNotificationRepository,
                              AbandonedCartRepository abandonedCartRepository,
                              LoyaltyTransactionRepository loyaltyTransactionRepository,
                              ReferralRepository referralRepository,
                              ReturnRequestRepository returnRequestRepository,
                              GiftCardRepository giftCardRepository,
                              NewsletterSubscriberRepository newsletterSubscriberRepository,
                              PrivacyConsentService privacyConsentService,
                              PaymentMethodService paymentMethodService,
                              AuditLogService auditLogService,
                              EmailService emailService,
                              @Value("${app.api-url:http://localhost:8585}") String apiUrl,
                              @Value("${app.privacy.request-ttl-hours:48}") int ttlHours) {
        this.dataRequestRepository = dataRequestRepository;
        this.customerRepository = customerRepository;
        this.savedAddressRepository = savedAddressRepository;
        this.savedPaymentMethodRepository = savedPaymentMethodRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.stockNotificationRepository = stockNotificationRepository;
        this.abandonedCartRepository = abandonedCartRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.referralRepository = referralRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.giftCardRepository = giftCardRepository;
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.privacyConsentService = privacyConsentService;
        this.paymentMethodService = paymentMethodService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.apiUrl = apiUrl == null ? "" : apiUrl.replaceAll("/+$", "");
        this.ttlHours = ttlHours;
    }

    // ------------------------------------------------------------------ submit

    /**
     * Raises a request and mails the confirmation link.
     *
     * <p>Deliberately returns the same shape whether or not we hold any data for the address: the
     * response must not become an oracle for "does this person shop here". An already-open request
     * of the same type is reused rather than duplicated, so repeatedly submitting the form cannot be
     * used to flood someone's inbox.
     */
    @Transactional
    public DataRequestView submit(DataRequestSubmission submission) {
        String email = normalize(submission.email());
        String type = normalizeType(submission.requestType());
        Long tenantId = TenantContext.currentTenantId();

        Optional<DataRequest> open = dataRequestRepository
                .findByEmailIgnoreCaseAndRequestTypeAndStatusAndTenantId(
                        email, type, DataRequest.STATUS_PENDING_VERIFICATION, tenantId)
                .stream()
                .filter(r -> !isExpired(r))
                .findFirst();
        if (open.isPresent()) {
            return DataRequestView.from(open.get());
        }

        DataRequest request = DataRequest.builder()
                .tenantId(tenantId)
                .email(email)
                .requestType(type)
                .status(DataRequest.STATUS_PENDING_VERIFICATION)
                .token(generateToken())
                .expiresAt(new Date(System.currentTimeMillis() + ttlHours * 3_600_000L))
                .build();
        DataRequest saved = dataRequestRepository.save(request);

        String confirmUrl = apiUrl + "/api/privacy/confirm?token=" + saved.getToken();
        emailService.sendDataRequestVerification(email, type, confirmUrl, ttlHours);
        if (!emailService.isEnabled()) {
            // Same graceful-degradation contract as the rest of the app: with no SMTP configured the
            // send is a no-op, which would strand the flow. Log the link so local/demo use still works.
            log.info("Email disabled — confirm the {} request for this address at: {}", type, confirmUrl);
        }
        auditLogService.record(null, "DATA_REQUEST_SUBMIT", "DataRequest",
                String.valueOf(saved.getId()), "type=" + type);
        return DataRequestView.from(saved);
    }

    // ----------------------------------------------------------------- confirm

    /**
     * Marks a token verified. This is the GET side of the flow, so it is deliberately
     * <strong>not</strong> the step that erases anything — corporate mail scanners and link
     * prefetchers routinely fetch every URL in an inbox, and a destructive GET would let one of them
     * wipe an account nobody ever clicked. Erasure needs the separate POST in {@link #erase}.
     */
    @Transactional
    public Optional<DataRequest> confirm(String token) {
        Optional<DataRequest> found = lookupUsable(token);
        found.ifPresent(request -> {
            if (DataRequest.STATUS_PENDING_VERIFICATION.equals(request.getStatus())) {
                request.setStatus(DataRequest.STATUS_VERIFIED);
                request.setVerifiedAt(new Date());
                dataRequestRepository.save(request);
            }
        });
        return found;
    }

    /** Looks up a token that is still actionable, expiring it in place if its TTL has lapsed. */
    @Transactional
    public Optional<DataRequest> lookupUsable(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<DataRequest> found = dataRequestRepository.findByToken(token.trim());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        DataRequest request = found.get();
        if (DataRequest.STATUS_COMPLETED.equals(request.getStatus())
                || DataRequest.STATUS_EXPIRED.equals(request.getStatus())) {
            return Optional.empty();
        }
        if (isExpired(request)) {
            request.setStatus(DataRequest.STATUS_EXPIRED);
            dataRequestRepository.save(request);
            return Optional.empty();
        }
        return Optional.of(request);
    }

    // ------------------------------------------------------------------ export

    /**
     * Builds the portable bundle and closes the request. Runs bound to the tenant recorded on the
     * request rather than the ambient one — the subject is clicking a link from their inbox, which
     * carries no {@code X-Tenant-Id} header, so the storefront they actually shopped on has to come
     * from the row. Same "derive from a resolved parent, not ambient context" rule
     * {@code ReturnService} and {@code FulfillmentService.trackShipments} already follow.
     */
    @Transactional
    public Optional<DataExportView> export(String token) {
        Optional<DataRequest> found = lookupUsable(token);
        if (found.isEmpty() || !DataRequest.TYPE_EXPORT.equals(found.get().getRequestType())) {
            return Optional.empty();
        }
        DataRequest request = found.get();
        if (!DataRequest.STATUS_VERIFIED.equals(request.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(withTenant(request.getTenantId(), () -> {
            DataExportView bundle = buildExport(request.getEmail(), request.getTenantId());
            request.setStatus(DataRequest.STATUS_COMPLETED);
            request.setCompletedAt(new Date());
            request.setResultSummary("Exported " + bundle.orders().size()
                    + " order(s) plus the linked profile, marketing and consent records.");
            dataRequestRepository.save(request);
            auditLogService.record(null, "DATA_REQUEST_EXPORT", "DataRequest",
                    String.valueOf(request.getId()), request.getResultSummary());
            return bundle;
        }));
    }

    private DataExportView buildExport(String email, Long tenantId) {
        Customer customer = customerRepository.findByEmailAndTenantId(email, tenantId);

        DataExportView.Profile profile = customer == null ? null : new DataExportView.Profile(
                customer.getFirstName(), customer.getLastName(), customer.getEmail(),
                customer.isNewsletterSubscribed(), customer.getLoyaltyPoints(),
                customer.getLifetimePoints(), customer.getReferralCode());

        List<DataExportView.OrderSummary> orders = customer == null ? List.of()
                : customer.getOrders().stream()
                .sorted(Comparator.comparing(Order::getId))
                .map(DataRequestService::toOrderSummary)
                .toList();

        List<DataExportView.SavedAddressEntry> addresses = savedAddressRepository
                .findByEmailIgnoreCaseAndTenantIdOrderByDefaultAddressDescIdDesc(email, tenantId).stream()
                .map(a -> new DataExportView.SavedAddressEntry(a.getLabel(), a.getRecipientName(),
                        a.getStreet(), a.getCity(), a.getState(), a.getCountry(), a.getZipCode(),
                        a.isDefaultAddress()))
                .toList();

        List<DataExportView.SavedCardEntry> cards = savedPaymentMethodRepository
                .findByEmailIgnoreCaseAndTenantIdOrderByDefaultMethodDescIdDesc(email, tenantId).stream()
                .map(c -> new DataExportView.SavedCardEntry(c.getBrand(), c.getLast4(),
                        c.getExpMonth(), c.getExpYear(), c.isDefaultMethod()))
                .toList();

        List<Long> wishlist = wishlistItemRepository.findByEmailAndTenantId(email, tenantId).stream()
                .map(WishlistItem::getProductId)
                .toList();

        List<DataExportView.StockWatchEntry> watches = stockNotificationRepository
                .findByEmailIgnoreCaseAndTenantId(email, tenantId).stream()
                .map(s -> new DataExportView.StockWatchEntry(s.getProductId(), s.getVariantSku(),
                        s.isNotified(), s.getDateCreated()))
                .toList();

        List<DataExportView.AbandonedCartEntry> carts = abandonedCartRepository
                .findByEmailIgnoreCaseAndTenantId(email, tenantId).stream()
                .map(c -> new DataExportView.AbandonedCartEntry(c.getItemCount(), c.getTotal(),
                        c.getSummary(), c.isRecovered(), c.getLastUpdated()))
                .toList();

        List<DataExportView.PointsEntry> points = loyaltyTransactionRepository
                .findByCustomerEmailIgnoreCaseAndTenantId(email, tenantId).stream()
                .map(t -> new DataExportView.PointsEntry(t.getType(), t.getPoints(),
                        t.getDescription(), t.getOrderId(), t.getDateCreated()))
                .toList();

        List<DataExportView.ReferralEntry> referrals = referralRepository
                .findByRefereeEmailIgnoreCaseAndTenantId(email, tenantId).stream()
                .map(r -> new DataExportView.ReferralEntry(r.getReferrerCode(), r.getStatus(),
                        r.getRefereePoints(), r.getDateCreated()))
                .toList();

        List<DataExportView.ReturnEntry> returns = returnRequestRepository
                .findByCustomerEmailIgnoreCaseAndTenantIdOrderByDateCreatedDesc(email, tenantId).stream()
                .map(r -> new DataExportView.ReturnEntry(r.getOrderTrackingNumber(), r.getReason(),
                        r.getStatus(), r.getRefundAmount(), r.getDateCreated()))
                .toList();

        List<DataExportView.GiftCardEntry> giftCards = giftCardRepository
                .findByRecipientEmailIgnoreCaseAndTenantId(email, tenantId).stream()
                .map(g -> new DataExportView.GiftCardEntry(g.getCode(), g.getBalance(), g.isActive(),
                        g.getDateCreated()))
                .toList();

        NewsletterSubscriber subscriber = newsletterSubscriberRepository.findByEmailAndTenantId(email, tenantId);
        DataExportView.NewsletterEntry newsletter = subscriber == null ? null
                : new DataExportView.NewsletterEntry(subscriber.getName(), subscriber.isSubscribed(),
                subscriber.getDateCreated());

        List<DataRequestView> previous = dataRequestRepository
                .findByEmailIgnoreCaseAndTenantIdOrderByIdDesc(email, tenantId).stream()
                .map(DataRequestView::from)
                .toList();

        return new DataExportView(
                new Date(), email, privacyConsentService.policyVersion(), EXPORT_NOTICE,
                profile, orders, addresses, cards, wishlist, watches, carts, points, referrals,
                returns, giftCards, newsletter,
                privacyConsentService.historyForEmail(email, tenantId), previous,
                List.of(REVIEWS_NOTICE));
    }

    private static DataExportView.OrderSummary toOrderSummary(Order order) {
        List<DataExportView.LineItem> items = order.getOrderItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(i -> new DataExportView.LineItem(i.getProductId(), i.getVariantSku(),
                        i.getVariantLabel(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new DataExportView.OrderSummary(
                order.getOrderTrackingNumber(), order.getStatus(), order.getTotalQuantity(),
                order.getTotalPrice(), order.getShippingMethod(), order.getShippingAmount(),
                order.getTaxAmount(), order.getDiscountAmount(), order.getDateCreated(),
                toAddressEntry(order.getShippingAddress()), toAddressEntry(order.getBillingAddress()),
                items);
    }

    private static DataExportView.AddressEntry toAddressEntry(Address address) {
        return address == null ? null : new DataExportView.AddressEntry(address.getStreet(),
                address.getCity(), address.getState(), address.getCountry(), address.getZipCode());
    }

    // ------------------------------------------------------------------- erase

    /**
     * Carries out an erasure. POST-only by design (see {@link #confirm}).
     *
     * <p>Order and order-item rows survive: they are the financial record, and deleting the customer
     * would cascade straight through them. What goes is the linkage and the marketing surface.
     */
    @Transactional
    public Optional<String> erase(String token) {
        Optional<DataRequest> found = lookupUsable(token);
        if (found.isEmpty() || !DataRequest.TYPE_ERASURE.equals(found.get().getRequestType())) {
            return Optional.empty();
        }
        DataRequest request = found.get();
        if (!DataRequest.STATUS_VERIFIED.equals(request.getStatus())) {
            // The subject must have opened the emailed link first; a bare POST is not proof of identity.
            return Optional.empty();
        }
        return Optional.of(withTenant(request.getTenantId(), () -> {
            String summary = eraseSubject(request.getEmail(), request.getTenantId());
            request.setStatus(DataRequest.STATUS_COMPLETED);
            request.setCompletedAt(new Date());
            request.setResultSummary(summary);
            // The request row keeps the email: it is the receipt proving the erasure was authorised,
            // and is itself erased if the subject ever returns and asks again.
            dataRequestRepository.save(request);
            auditLogService.record(null, "DATA_REQUEST_ERASURE", "DataRequest",
                    String.valueOf(request.getId()), summary);
            return summary;
        }));
    }

    private String eraseSubject(String email, Long tenantId) {
        List<String> done = new ArrayList<>();

        // --- Deleted outright: marketing and convenience data with no retention basis. ---
        List<WishlistItem> wishlist = wishlistItemRepository.findByEmailAndTenantId(email, tenantId);
        wishlistItemRepository.deleteAll(wishlist);
        count(done, wishlist.size(), "wishlist item");

        List<StockNotification> watches = stockNotificationRepository.findByEmailIgnoreCaseAndTenantId(email, tenantId);
        stockNotificationRepository.deleteAll(watches);
        count(done, watches.size(), "back-in-stock alert");

        List<AbandonedCart> carts = abandonedCartRepository.findByEmailIgnoreCaseAndTenantId(email, tenantId);
        abandonedCartRepository.deleteAll(carts);
        count(done, carts.size(), "saved cart");

        List<SavedAddress> addresses = savedAddressRepository
                .findByEmailIgnoreCaseAndTenantIdOrderByDefaultAddressDescIdDesc(email, tenantId);
        savedAddressRepository.deleteAll(addresses);
        count(done, addresses.size(), "saved address");

        // Routed through PaymentMethodService so each card is also detached at Stripe, not just
        // dropped locally — deleting our row alone would leave the card attached to the processor.
        List<SavedPaymentMethod> cards = savedPaymentMethodRepository
                .findByEmailIgnoreCaseAndTenantIdOrderByDefaultMethodDescIdDesc(email, tenantId);
        cards.forEach(card -> paymentMethodService.delete(email, card.getId()));
        count(done, cards.size(), "saved card");

        NewsletterSubscriber subscriber = newsletterSubscriberRepository.findByEmailAndTenantId(email, tenantId);
        if (subscriber != null) {
            newsletterSubscriberRepository.delete(subscriber);
            done.add("removed the newsletter subscription");
        }

        int consents = privacyConsentService.deleteForEmail(email, tenantId);
        count(done, consents, "consent record");

        // --- Retained but de-linked: rows with a financial or legal retention basis. ---
        List<LoyaltyTransaction> points = loyaltyTransactionRepository
                .findByCustomerEmailIgnoreCaseAndTenantId(email, tenantId);
        points.forEach(t -> t.setCustomerEmail(tombstoneEmail(t.getId())));
        loyaltyTransactionRepository.saveAll(points);
        count(done, points.size(), "rewards ledger entry (anonymized)");

        List<ReturnRequest> returns = returnRequestRepository
                .findByCustomerEmailIgnoreCaseAndTenantIdOrderByDateCreatedDesc(email, tenantId);
        returns.forEach(r -> r.setCustomerEmail(tombstoneEmail(r.getId())));
        returnRequestRepository.saveAll(returns);
        count(done, returns.size(), "return request (anonymized)");

        List<Referral> referrals = referralRepository.findByRefereeEmailIgnoreCaseAndTenantId(email, tenantId);
        referrals.forEach(r -> r.setRefereeEmail(tombstoneEmail(r.getId())));
        referralRepository.saveAll(referrals);
        count(done, referrals.size(), "referral (anonymized)");

        // Gift cards are bearer store credit — real money. The recipient's address is scrubbed but
        // the card keeps its balance, because voiding it would confiscate funds, not protect privacy.
        List<GiftCard> giftCards = giftCardRepository.findByRecipientEmailIgnoreCaseAndTenantId(email, tenantId);
        giftCards.forEach(g -> g.setRecipientEmail(null));
        giftCardRepository.saveAll(giftCards);
        count(done, giftCards.size(), "gift card (recipient cleared, balance kept)");

        // --- The customer record itself: tombstoned, never deleted (CascadeType.ALL on orders). ---
        Customer customer = customerRepository.findByEmailAndTenantId(email, tenantId);
        if (customer != null) {
            int orderCount = customer.getOrders().size();
            customer.getOrders().forEach(DataRequestService::scrubOrderAddresses);
            customer.setFirstName(ERASED_NAME);
            customer.setLastName(ERASED_NAME);
            customer.setEmail(tombstoneEmail(customer.getId()));
            customer.setNewsletterSubscribed(false);
            customer.setUnsubscribeToken(null);
            customer.setReferralCode(null);
            customer.setLoyaltyPoints(0);
            customer.setLifetimePoints(0);
            customerRepository.save(customer);
            done.add("anonymized the customer profile");
            if (orderCount > 0) {
                done.add(orderCount + " order" + (orderCount == 1 ? "" : "s")
                        + " retained as financial records, with street addresses cleared and contact details removed");
            }
        }

        if (done.isEmpty()) {
            return "No personal data was found for this address.";
        }
        return String.join("; ", done) + ". " + REVIEWS_NOTICE;
    }

    /**
     * Clears the street line but keeps city/state/country/postcode. The reason orders are retained
     * at all is proving where a sale was taxed, and that needs the jurisdiction, not the doorstep.
     */
    private static void scrubOrderAddresses(Order order) {
        clearStreet(order.getShippingAddress());
        clearStreet(order.getBillingAddress());
    }

    private static void clearStreet(Address address) {
        if (address != null) {
            address.setStreet(null);
        }
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Runs an operation bound to a specific tenant, restoring whatever was bound before. Needed
     * because these flows are entered from an emailed link with no tenant header, while the
     * repositories below are all tenant-scoped (roadmap #21).
     */
    private <T> T withTenant(Long tenantId, Supplier<T> work) {
        Long previous = TenantContext.currentTenantId();
        TenantContext.set(tenantId);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    private static void count(List<String> done, int n, String noun) {
        if (n > 0) {
            done.add("deleted " + n + " " + noun + (n == 1 ? "" : "s"));
        }
    }

    /** Unique per row so tombstoning never trips a unique index by writing the same value twice. */
    private static String tombstoneEmail(Long id) {
        return "erased-" + (id == null ? "x" : id) + ERASED_DOMAIN;
    }

    private boolean isExpired(DataRequest request) {
        return request.getExpiresAt() != null && request.getExpiresAt().before(new Date());
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String normalizeType(String requestType) {
        String value = requestType == null ? "" : requestType.trim().toUpperCase();
        if (DataRequest.TYPE_EXPORT.equals(value) || DataRequest.TYPE_ERASURE.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Unknown request type: " + requestType);
    }
}
