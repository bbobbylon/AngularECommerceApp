package com.bob.ecommerceangularapp.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Everything the store holds about one data subject, in a portable, self-describing shape
 * (GDPR Art. 15 access / Art. 20 portability — roadmap #24).
 *
 * <p>Typed sub-records rather than a loose {@code Map} so the export's contents are documented by
 * the code itself: adding a PII-bearing entity to the app without extending this file is a visible
 * omission at compile time rather than a silently incomplete export.
 *
 * <p>{@code notIncluded} is deliberately part of the payload. An export that quietly omits a
 * category is indistinguishable from one where the subject simply had no such data, so the bundle
 * says out loud what was left out and why — most importantly reviews, which this schema stores
 * against a display name only, with no email or customer link to match on.
 */
public record DataExportView(
        Date generatedAt,
        String email,
        String policyVersion,
        String notice,
        Profile profile,
        List<OrderSummary> orders,
        List<SavedAddressEntry> savedAddresses,
        List<SavedCardEntry> savedCards,
        List<Long> wishlistProductIds,
        List<StockWatchEntry> stockNotifications,
        List<AbandonedCartEntry> abandonedCarts,
        List<PointsEntry> loyaltyLedger,
        List<ReferralEntry> referrals,
        List<ReturnEntry> returnRequests,
        List<GiftCardEntry> giftCards,
        NewsletterEntry newsletter,
        List<ConsentView> consentHistory,
        List<DataRequestView> previousRequests,
        List<String> notIncluded) {

    /** The customer record itself. Null when the subject is a subscriber who never ordered. */
    public record Profile(
            String firstName,
            String lastName,
            String email,
            boolean newsletterSubscribed,
            Integer loyaltyPoints,
            Integer lifetimePoints,
            String referralCode) {
    }

    /** One order with its line items and the addresses it shipped to/was billed at. */
    public record OrderSummary(
            String orderTrackingNumber,
            String status,
            int totalQuantity,
            BigDecimal totalPrice,
            String shippingMethod,
            BigDecimal shippingAmount,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            Date dateCreated,
            AddressEntry shippingAddress,
            AddressEntry billingAddress,
            List<LineItem> items) {
    }

    public record LineItem(
            Long productId,
            String variantSku,
            String variantLabel,
            int quantity,
            BigDecimal unitPrice) {
    }

    public record AddressEntry(
            String street,
            String city,
            String state,
            String country,
            String zipCode) {
    }

    public record SavedAddressEntry(
            String label,
            String recipientName,
            String street,
            String city,
            String state,
            String country,
            String zipCode,
            boolean defaultAddress) {
    }

    /**
     * Card references only. This store never held a card number — {@code SavedPaymentMethod} keeps a
     * Stripe reference plus the brand/last four, which is all PCI allows and all there is to export.
     */
    public record SavedCardEntry(
            String brand,
            String last4,
            Integer expMonth,
            Integer expYear,
            boolean defaultMethod) {
    }

    public record StockWatchEntry(
            Long productId,
            String variantSku,
            boolean notified,
            Date dateCreated) {
    }

    public record AbandonedCartEntry(
            int itemCount,
            BigDecimal total,
            String summary,
            boolean recovered,
            Date lastUpdated) {
    }

    public record PointsEntry(
            String type,
            int points,
            String description,
            Long orderId,
            Date dateCreated) {
    }

    public record ReferralEntry(
            String referrerCode,
            String status,
            int refereePoints,
            Date dateCreated) {
    }

    public record ReturnEntry(
            String orderTrackingNumber,
            String reason,
            String status,
            BigDecimal refundAmount,
            Date dateCreated) {
    }

    /** Balance is included because store credit is the subject's money, not just their data. */
    public record GiftCardEntry(
            String code,
            BigDecimal balance,
            boolean active,
            Date dateCreated) {
    }

    public record NewsletterEntry(
            String name,
            boolean subscribed,
            Date dateCreated) {
    }
}
