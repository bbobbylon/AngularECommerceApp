package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.SavedPaymentMethodRepository;
import com.bob.ecommerceangularapp.dto.SetupIntentResponse;
import com.bob.ecommerceangularapp.entity.SavedPaymentMethod;
import com.stripe.Stripe;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saved payment methods. Stores only Stripe references + display metadata (brand/last4/expiry) — the
 * actual card lives in Stripe (PCI). Adding a card uses a Stripe SetupIntent; everything is gated on a
 * configured Stripe key and degrades gracefully (list/remove always work; add is disabled without Stripe).
 */
@Service
public class PaymentMethodService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodService.class);

    private final SavedPaymentMethodRepository repository;
    private final String stripeKey;

    public PaymentMethodService(SavedPaymentMethodRepository repository,
                                @Value("${stripe.key.secret}") String stripeKey) {
        this.repository = repository;
        this.stripeKey = stripeKey;
    }

    private boolean stripeConfigured() {
        return stripeKey != null && !stripeKey.isBlank();
    }

    @Transactional(readOnly = true)
    public List<SavedPaymentMethod> list(String email) {
        return email == null ? List.of()
                : repository.findByEmailIgnoreCaseOrderByDefaultMethodDescIdDesc(email.trim());
    }

    /** Begins an "add a card" flow; returns a disabled response when Stripe isn't configured. */
    public SetupIntentResponse createSetupIntent(String email) {
        if (!stripeConfigured()) {
            return new SetupIntentResponse(false, null);
        }
        try {
            Stripe.apiKey = stripeKey;
            Map<String, Object> params = new HashMap<>();
            params.put("usage", "off_session");
            params.put("payment_method_types", List.of("card"));
            SetupIntent intent = SetupIntent.create(params);
            return new SetupIntentResponse(true, intent.getClientSecret());
        } catch (Exception e) {
            log.warn("Failed to create SetupIntent for {}: {}", email, e.getMessage());
            return new SetupIntentResponse(false, null);
        }
    }

    /** Records a confirmed Stripe PaymentMethod as a saved card (fetches brand/last4/expiry from Stripe). */
    @Transactional
    public SavedPaymentMethod record(String email, String paymentMethodId) {
        if (!stripeConfigured()) {
            throw new IllegalStateException("Card saving isn't available — payments aren't configured.");
        }
        try {
            Stripe.apiKey = stripeKey;
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            SavedPaymentMethod saved = new SavedPaymentMethod();
            saved.setEmail(email.trim());
            saved.setStripePaymentMethodId(pm.getId());
            if (pm.getCard() != null) {
                saved.setBrand(pm.getCard().getBrand());
                saved.setLast4(pm.getCard().getLast4());
                saved.setExpMonth(pm.getCard().getExpMonth() == null ? null : pm.getCard().getExpMonth().intValue());
                saved.setExpYear(pm.getCard().getExpYear() == null ? null : pm.getCard().getExpYear().intValue());
            }
            saved.setDefaultMethod(repository.findByEmailIgnoreCaseOrderByDefaultMethodDescIdDesc(email.trim()).isEmpty());
            return repository.save(saved);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not save that card: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void delete(String email, Long id) {
        repository.findById(id)
                .filter(pm -> pm.getEmail() != null && pm.getEmail().equalsIgnoreCase(email.trim()))
                .ifPresent(pm -> {
                    detachFromStripe(pm.getStripePaymentMethodId());
                    repository.delete(pm);
                });
    }

    private void detachFromStripe(String paymentMethodId) {
        if (!stripeConfigured() || paymentMethodId == null) {
            return;
        }
        try {
            Stripe.apiKey = stripeKey;
            PaymentMethod.retrieve(paymentMethodId).detach();
        } catch (Exception e) {
            log.warn("Could not detach payment method {} from Stripe: {}", paymentMethodId, e.getMessage());
        }
    }
}
