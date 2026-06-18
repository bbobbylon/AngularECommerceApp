package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.ReferralRepository;
import com.bob.ecommerceangularapp.dto.ReferralSummary;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Referral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Referral program. Each customer has a shareable {@code referralCode}; when a NEW customer places
 * their first order carrying that code, both sides are rewarded with loyalty points (reusing
 * {@link LoyaltyService}). One referral per referee, no self-referrals. Codes are assigned lazily.
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final int REFERRER_REWARD = 500;   // points to the existing customer
    public static final int REFEREE_REWARD = 200;     // points to the new customer

    private final CustomerRepository customerRepository;
    private final ReferralRepository referralRepository;
    private final LoyaltyService loyaltyService;

    public ReferralService(CustomerRepository customerRepository,
                           ReferralRepository referralRepository,
                           LoyaltyService loyaltyService) {
        this.customerRepository = customerRepository;
        this.referralRepository = referralRepository;
        this.loyaltyService = loyaltyService;
    }

    /** Summary for a customer (assigning their code on first view). Empty code for unknown emails. */
    @Transactional
    public ReferralSummary summary(String email) {
        Customer customer = email == null ? null : customerRepository.findByEmail(email);
        if (customer == null) {
            return new ReferralSummary(email, null, 0, 0, REFERRER_REWARD, REFEREE_REWARD);
        }
        String code = ensureCode(customer);
        List<Referral> referrals = referralRepository.findByReferrerCode(code);
        int pointsEarned = referrals.stream().mapToInt(Referral::getReferrerPoints).sum();
        return new ReferralSummary(customer.getEmail(), code, referrals.size(), pointsEarned,
                REFERRER_REWARD, REFEREE_REWARD);
    }

    /**
     * Records a referral when {@code referee} places their first order with {@code referrerCode}, and
     * grants both rewards. No-op (logged) if the code is blank/unknown, self-referral, or the referee
     * was already referred. Called inside the checkout transaction.
     */
    @Transactional
    public void recordReferral(Customer referee, String referrerCode, Long orderId) {
        if (referrerCode == null || referrerCode.isBlank() || referee == null) {
            return;
        }
        if (referralRepository.existsByRefereeEmailIgnoreCase(referee.getEmail())) {
            return; // already referred
        }
        Customer referrer = customerRepository.findByReferralCode(referrerCode.trim().toUpperCase());
        if (referrer == null || referrer.getEmail().equalsIgnoreCase(referee.getEmail())) {
            return; // unknown code or self-referral
        }

        Referral referral = new Referral();
        referral.setReferrerCode(referrer.getReferralCode());
        referral.setRefereeEmail(referee.getEmail());
        referral.setStatus("COMPLETED");
        referral.setReferrerPoints(REFERRER_REWARD);
        referral.setRefereePoints(REFEREE_REWARD);
        referral.setOrderId(orderId);
        referralRepository.save(referral);

        loyaltyService.grantPoints(referrer, REFERRER_REWARD, "Referral reward: " + referee.getEmail());
        loyaltyService.grantPoints(referee, REFEREE_REWARD, "Welcome referral bonus");
        log.info("Recorded referral: {} -> {}", referrer.getReferralCode(), referee.getEmail());
    }

    private String ensureCode(Customer customer) {
        if (customer.getReferralCode() != null && !customer.getReferralCode().isBlank()) {
            return customer.getReferralCode();
        }
        String code;
        do {
            code = "REF" + randomGroup();
        } while (customerRepository.existsByReferralCode(code));
        customer.setReferralCode(code);
        customerRepository.save(customer);
        return code;
    }

    private String randomGroup() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
