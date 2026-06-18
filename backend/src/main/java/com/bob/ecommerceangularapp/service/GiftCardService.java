package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.GiftCardRepository;
import com.bob.ecommerceangularapp.dto.AdminGiftCardRequest;
import com.bob.ecommerceangularapp.dto.GiftCardView;
import com.bob.ecommerceangularapp.entity.GiftCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

/**
 * Gift cards / store credit. {@link #check} previews a card's usable balance for the checkout; at order
 * time {@link #redeem} draws the applied amount down atomically. Issued/managed by admins. Redemption
 * is clamped to the remaining balance, so a card can never go negative or over-apply.
 */
@Service
public class GiftCardService {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GiftCardRepository giftCardRepository;

    public GiftCardService(GiftCardRepository giftCardRepository) {
        this.giftCardRepository = giftCardRepository;
    }

    /** Non-mutating check used by the checkout to show the available balance. */
    @Transactional(readOnly = true)
    public GiftCardView check(String code) {
        if (code == null || code.isBlank()) {
            return GiftCardView.invalid(code, "Enter a gift card code.");
        }
        GiftCard card = giftCardRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (card == null || !card.isActive()) {
            return GiftCardView.invalid(code, "That gift card code isn't valid.");
        }
        if (card.getBalance() == null || card.getBalance().signum() <= 0) {
            return GiftCardView.invalid(code, "This gift card has no balance left.");
        }
        return new GiftCardView(true, card.getCode(), card.getBalance(), "Gift card applied.");
    }

    /**
     * Applies a gift card to an order total, drawing down the balance. Returns the amount actually
     * applied: {@code min(balance, total)} (zero if the card is invalid/empty). Call inside the order
     * transaction so the decrement commits with the order.
     */
    @Transactional
    public BigDecimal redeem(String code, BigDecimal orderTotal) {
        if (code == null || code.isBlank() || orderTotal == null || orderTotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        GiftCard card = giftCardRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (card == null || !card.isActive() || card.getBalance() == null || card.getBalance().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal applied = card.getBalance().min(orderTotal);
        card.setBalance(card.getBalance().subtract(applied));
        giftCardRepository.save(card);
        return applied;
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<GiftCard> listAll() {
        return giftCardRepository.findAll();
    }

    @Transactional
    public GiftCard issue(AdminGiftCardRequest request) {
        String code = (request.code() == null || request.code().isBlank())
                ? generateUniqueCode() : request.code().trim().toUpperCase();
        GiftCard card = GiftCard.builder()
                .code(code)
                .initialBalance(request.initialBalance())
                .balance(request.initialBalance())
                .recipientEmail(request.recipientEmail() == null || request.recipientEmail().isBlank()
                        ? null : request.recipientEmail().trim())
                .active(request.active())
                .build();
        return giftCardRepository.save(card);
    }

    @Transactional
    public void deactivate(Long id) {
        GiftCard card = giftCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gift card not found: " + id));
        card.setActive(false);
        giftCardRepository.save(card);
    }

    /** Human-friendly grouped code, e.g. GIFT-AB12-CD34. */
    private String generateUniqueCode() {
        String code;
        do {
            code = "GIFT-" + randomGroup() + "-" + randomGroup();
        } while (giftCardRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    private String randomGroup() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
