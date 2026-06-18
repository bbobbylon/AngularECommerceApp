package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.AbandonedCartRepository;
import com.bob.ecommerceangularapp.dto.AbandonedCartRequest;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.AbandonedCart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Abandoned-cart recovery. The checkout captures a cart snapshot (by email) before it's completed;
 * if no order follows, a scheduler emails a recovery nudge once the cart has been idle for
 * {@code app.abandoned-cart.after-minutes}. Placing an order marks the cart recovered. Email is gated
 * by {@link EmailService}, so this is a safe no-op without SMTP.
 */
@Service
public class AbandonedCartService {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartService.class);

    private final AbandonedCartRepository repository;
    private final EmailService emailService;
    private final long afterMinutes;

    public AbandonedCartService(AbandonedCartRepository repository,
                                EmailService emailService,
                                @Value("${app.abandoned-cart.after-minutes:60}") long afterMinutes) {
        this.repository = repository;
        this.emailService = emailService;
        this.afterMinutes = afterMinutes;
    }

    /** Upserts the live (unrecovered) cart snapshot for an email; resets the reminder on each change. */
    @Transactional
    public void capture(AbandonedCartRequest request) {
        if (request.email() == null || request.email().isBlank() || request.itemCount() <= 0) {
            return;
        }
        AbandonedCart cart = repository
                .findFirstByEmailIgnoreCaseAndRecoveredFalseOrderByIdDesc(request.email().trim())
                .orElseGet(AbandonedCart::new);
        cart.setEmail(request.email().trim());
        cart.setItemCount(request.itemCount());
        cart.setTotal(request.total() == null ? BigDecimal.ZERO : request.total());
        cart.setSummary(request.summary());
        cart.setRecovered(false);
        cart.setReminded(false); // fresh activity → eligible for a (new) reminder once idle
        repository.save(cart);
    }

    /** Marks every live cart for an email as recovered (called when they place an order). */
    @Transactional
    public void markRecovered(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        List<AbandonedCart> carts = repository.findByEmailIgnoreCaseAndRecoveredFalse(email);
        carts.forEach(c -> c.setRecovered(true));
        repository.saveAll(carts);
    }

    /** Emails a recovery nudge for carts idle past the threshold; marks them reminded. Returns count. */
    @Transactional
    public int remindStale() {
        Date cutoff = new Date(System.currentTimeMillis() - afterMinutes * 60_000L);
        List<AbandonedCart> stale = repository
                .findByRecoveredFalseAndRemindedFalseAndLastUpdatedBefore(cutoff);
        for (AbandonedCart cart : stale) {
            emailService.sendAbandonedCart(cart.getEmail(), cart.getItemCount(),
                    cart.getTotal() == null ? BigDecimal.ZERO : cart.getTotal());
            cart.setReminded(true);
        }
        repository.saveAll(stale);
        if (!stale.isEmpty()) {
            log.info("Sent {} abandoned-cart reminder(s).", stale.size());
        }
        return stale.size();
    }
}
