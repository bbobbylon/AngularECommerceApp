package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.PromotionRepository;
import com.bob.ecommerceangularapp.dto.AppliedPromotion;
import com.bob.ecommerceangularapp.dto.PromotionRequest;
import com.bob.ecommerceangularapp.entity.Promotion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Automatic, no-code promotions: unlike {@link CouponService}, nothing is entered by the customer —
 * {@link #findBest} is consulted by {@link TaxShippingService#quote} for every checkout and returns
 * whichever active, in-window, min-spend-eligible promotion is worth the most, so it can stack
 * alongside any manually-entered coupon code.
 */
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /** The single highest-value promotion that currently applies to this subtotal, if any. */
    @Transactional(readOnly = true)
    public Optional<AppliedPromotion> findBest(BigDecimal subtotal) {
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        LocalDate today = LocalDate.now();

        return promotionRepository.findByActiveTrue().stream()
                .filter(p -> p.getStartsAt() == null || !p.getStartsAt().isAfter(today))
                .filter(p -> p.getEndsAt() == null || !p.getEndsAt().isBefore(today))
                .filter(p -> p.getMinSpend() == null || safeSubtotal.compareTo(p.getMinSpend()) >= 0)
                .map(p -> new AppliedPromotion(p.getName(), discountFor(p, safeSubtotal)))
                .filter(applied -> applied.discount().signum() > 0)
                .max(Comparator.comparing(AppliedPromotion::discount));
    }

    private BigDecimal discountFor(Promotion promotion, BigDecimal subtotal) {
        BigDecimal discount = BigDecimal.ZERO;
        if (promotion.getPercentOff() != null && promotion.getPercentOff() > 0) {
            discount = subtotal.multiply(BigDecimal.valueOf(promotion.getPercentOff()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (promotion.getAmountOff() != null) {
            discount = promotion.getAmountOff();
        }
        // never discount more than the subtotal
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    // ----- admin -----

    @Transactional(readOnly = true)
    public List<Promotion> list() {
        return promotionRepository.findAll();
    }

    @Transactional
    public Promotion save(PromotionRequest request) {
        Promotion promotion = request.id() != null
                ? promotionRepository.findById(request.id())
                        .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + request.id()))
                : new Promotion();
        promotion.setName(request.name().trim());
        promotion.setDescription(request.description());
        promotion.setPercentOff(request.percentOff());
        promotion.setAmountOff(request.amountOff());
        promotion.setMinSpend(request.minSpend());
        promotion.setActive(request.active());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        return promotionRepository.save(promotion);
    }

    @Transactional
    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Promotion not found: " + id);
        }
        promotionRepository.deleteById(id);
    }
}
