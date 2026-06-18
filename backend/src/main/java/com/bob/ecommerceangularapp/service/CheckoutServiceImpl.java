package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dto.PaymentInfo;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.dto.QuoteRequest;
import com.bob.ecommerceangularapp.dto.QuoteResponse;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Address;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final TaxShippingService taxShippingService;
    private final ProductVariantService productVariantService;
    private final GiftCardService giftCardService;
    private final LoyaltyService loyaltyService;
    private final ReferralService referralService;

    public CheckoutServiceImpl(CustomerRepository customerRepository,
                               EmailService emailService,
                               TaxShippingService taxShippingService,
                               ProductVariantService productVariantService,
                               GiftCardService giftCardService,
                               LoyaltyService loyaltyService,
                               ReferralService referralService,
                               @Value("${stripe.key.secret}") String secretKey) {
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.taxShippingService = taxShippingService;
        this.productVariantService = productVariantService;
        this.giftCardService = giftCardService;
        this.loyaltyService = loyaltyService;
        this.referralService = referralService;
        // Stripe is keyed globally via a static field.
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional
    public PurchaseResponse placeOrder(Purchase purchase) {

        Order order = purchase.getOrder();

        String orderTrackingNumber = generateOrderTrackingNumber();
        order.setOrderTrackingNumber(orderTrackingNumber);
        order.setPaymentIntentId(purchase.getPaymentIntentId());

        // populate order with its items (maintains the bidirectional link)
        Set<OrderItem> orderItems = purchase.getOrderItems();
        orderItems.forEach(order::add);

        // Draw down SKU-level inventory for any lines bought by variant (no-op for single-SKU items).
        productVariantService.decrementForOrderItems(orderItems);

        // populate order with its addresses
        order.setShippingAddress(purchase.getShippingAddress());
        order.setBillingAddress(purchase.getBillingAddress());

        // Recompute the total authoritatively from the subtotal: re-validate any coupon, then add
        // server-side shipping + tax (the same quote the storefront showed). Legacy/demo callers that
        // don't send a subtotal keep the total they posted. See TaxShippingService.
        if (purchase.getSubtotal() != null) {
            Address ship = purchase.getShippingAddress();
            QuoteResponse quote = taxShippingService.quote(new QuoteRequest(
                    purchase.getSubtotal(),
                    ship == null ? null : ship.getCountry(),
                    ship == null ? null : ship.getState(),
                    purchase.getCouponCode(),
                    purchase.getShippingMethodCode()));
            if (quote.discount() != null && quote.discount().signum() > 0) {
                order.setCouponCode(purchase.getCouponCode());
                order.setDiscountAmount(quote.discount());
            }
            order.setShippingAmount(quote.shippingAmount());
            order.setShippingMethod(quote.shippingMethodCode());
            order.setTaxAmount(quote.taxAmount());
            order.setTotalPrice(quote.total());

            // Redeem any gift card as store credit against the order total (clamped to its balance).
            if (purchase.getGiftCardCode() != null && !purchase.getGiftCardCode().isBlank()) {
                java.math.BigDecimal applied = giftCardService.redeem(purchase.getGiftCardCode(), quote.total());
                if (applied.signum() > 0) {
                    order.setGiftCardCode(purchase.getGiftCardCode());
                    order.setGiftCardAmount(applied);
                }
            }
        }

        // populate customer with the order, reusing an existing customer if the email matches
        Customer customer = purchase.getCustomer();
        Customer existingCustomer = customerRepository.findByEmail(customer.getEmail());
        if (existingCustomer != null) {
            customer = existingCustomer;
        }

        // Apply the checkout newsletter opt-in and track whether this is a fresh subscription
        // (newly created account, or a previously-unsubscribed customer re-opting-in).
        boolean wasSubscribed = customer.isNewsletterSubscribed() && existingCustomer != null;
        customer.setNewsletterSubscribed(purchase.isSubscribeToNewsletter());
        // Every customer gets a token (even opt-outs) so token-presence means "processed" — this lets
        // the startup backfill safely subscribe only pre-existing rows without re-subscribing opt-outs.
        customer.ensureUnsubscribeToken();

        customer.add(order);

        // cascade persists the order, items and addresses
        Customer saved = customerRepository.save(customer);

        // Loyalty: redeem requested points as store credit (server-validated), then earn on this order.
        // Runs after the save so the order has an id for the ledger; same transaction, so it commits atomically.
        if (purchase.getPointsToRedeem() > 0) {
            loyaltyService.redeem(saved, order, purchase.getPointsToRedeem());
        }
        loyaltyService.award(saved, order);

        // Referral: reward both parties when a NEW customer (this is their first order) used a code.
        if (saved.getOrders().size() == 1) {
            referralService.recordReferral(saved, purchase.getReferralCode(), order.getId());
        }

        // Email is gated inside EmailService — these are safe no-ops when SMTP isn't configured.
        emailService.sendOrderConfirmation(saved.getEmail(), saved.getFirstName(),
                orderTrackingNumber, order.getTotalPrice());
        if (saved.isNewsletterSubscribed() && !wasSubscribed) {
            emailService.sendWelcome(saved.getEmail(), saved.getFirstName());
        }

        return new PurchaseResponse(orderTrackingNumber);
    }

    private String generateOrderTrackingNumber() {
        return UUID.randomUUID().toString();
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentInfo paymentInfo) throws StripeException {
        List<String> paymentMethodTypes = new ArrayList<>();
        paymentMethodTypes.add("card");

        Map<String, Object> params = new HashMap<>();
        params.put("amount", paymentInfo.getAmount());
        params.put("currency", paymentInfo.getCurrency());
        params.put("payment_method_types", paymentMethodTypes);
        params.put("description", "Luv2Shop purchase");
        if (paymentInfo.getReceiptEmail() != null) {
            params.put("receipt_email", paymentInfo.getReceiptEmail());
        }

        return PaymentIntent.create(params);
    }
}
