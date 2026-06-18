package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.LoyaltyTransactionRepository;
import com.bob.ecommerceangularapp.dto.LoyaltySummary;
import com.bob.ecommerceangularapp.dto.LoyaltyTransactionView;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.LoyaltyTransaction;
import com.bob.ecommerceangularapp.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Loyalty / rewards points. Customers earn {@value #POINTS_PER_DOLLAR} point per $1 spent and redeem
 * points as store credit at {@value #CENTS_PER_POINT}¢ each (e.g. 250 pts = $2.50). Lifetime points
 * drive a Bronze/Silver/Gold tier. Earn + redeem are invoked from checkout inside the order
 * transaction; the running balance lives on {@link Customer} and every change is logged to the ledger.
 */
@Service
public class LoyaltyService {

    public static final int POINTS_PER_DOLLAR = 1;
    public static final int CENTS_PER_POINT = 1;          // 1 point = $0.01
    private static final int SILVER_AT = 500;
    private static final int GOLD_AT = 2000;

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository ledger;

    public LoyaltyService(CustomerRepository customerRepository, LoyaltyTransactionRepository ledger) {
        this.customerRepository = customerRepository;
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public LoyaltySummary summary(String email) {
        Customer customer = email == null ? null : customerRepository.findByEmail(email);
        int balance = customer == null ? 0 : nz(customer.getLoyaltyPoints());
        int lifetime = customer == null ? 0 : nz(customer.getLifetimePoints());
        List<LoyaltyTransactionView> history = (email == null ? List.<LoyaltyTransaction>of()
                : ledger.findTop20ByCustomerEmailIgnoreCaseOrderByDateCreatedDesc(email)).stream()
                .map(t -> new LoyaltyTransactionView(t.getType(), t.getPoints(), t.getDescription(), t.getDateCreated()))
                .toList();
        return new LoyaltySummary(email, balance, lifetime, tierFor(lifetime), nextTierFor(lifetime),
                pointsToNextTier(lifetime), pointsToMoney(balance), history);
    }

    /** Awards points for a placed order (floor of the order total). Returns the points earned. */
    @Transactional
    public int award(Customer customer, Order order) {
        if (customer == null || order == null || order.getTotalPrice() == null) {
            return 0;
        }
        int earned = order.getTotalPrice().setScale(0, RoundingMode.DOWN).intValue() * POINTS_PER_DOLLAR;
        if (earned <= 0) {
            return 0;
        }
        customer.setLoyaltyPoints(nz(customer.getLoyaltyPoints()) + earned);
        customer.setLifetimePoints(nz(customer.getLifetimePoints()) + earned);
        customerRepository.save(customer);
        record(customer.getEmail(), "EARN", earned, "Points for order " + order.getOrderTrackingNumber(), order.getId());
        return earned;
    }

    /**
     * Redeems up to {@code requestedPoints} of the customer's balance as store credit on the order,
     * capped by the balance and the order total. Records the redemption on the order + ledger and
     * returns the discount applied.
     */
    @Transactional
    public BigDecimal redeem(Customer customer, Order order, int requestedPoints) {
        if (customer == null || order == null || requestedPoints <= 0 || order.getTotalPrice() == null) {
            return BigDecimal.ZERO;
        }
        int balance = nz(customer.getLoyaltyPoints());
        int maxForOrder = order.getTotalPrice().movePointRight(2).intValue() / CENTS_PER_POINT; // can't exceed total
        int points = Math.min(Math.min(requestedPoints, balance), maxForOrder);
        if (points <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = pointsToMoney(points);
        customer.setLoyaltyPoints(balance - points);
        customerRepository.save(customer);
        order.setLoyaltyPointsRedeemed(points);
        order.setLoyaltyDiscount(discount);
        record(customer.getEmail(), "REDEEM", points, "Redeemed on order " + order.getOrderTrackingNumber(), order.getId());
        return discount;
    }

    /** Directly grants points to a customer (e.g. a referral reward) and logs it. Returns points granted. */
    @Transactional
    public int grantPoints(Customer customer, int points, String description) {
        if (customer == null || points <= 0) {
            return 0;
        }
        customer.setLoyaltyPoints(nz(customer.getLoyaltyPoints()) + points);
        customer.setLifetimePoints(nz(customer.getLifetimePoints()) + points);
        customerRepository.save(customer);
        record(customer.getEmail(), "EARN", points, description, null);
        return points;
    }

    private void record(String email, String type, int points, String description, Long orderId) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setCustomerEmail(email);
        tx.setType(type);
        tx.setPoints(points);
        tx.setDescription(description);
        tx.setOrderId(orderId);
        ledger.save(tx);
    }

    private BigDecimal pointsToMoney(int points) {
        return BigDecimal.valueOf((long) points * CENTS_PER_POINT, 2);
    }

    private String tierFor(int lifetime) {
        if (lifetime >= GOLD_AT) {
            return "Gold";
        }
        return lifetime >= SILVER_AT ? "Silver" : "Bronze";
    }

    private String nextTierFor(int lifetime) {
        if (lifetime >= GOLD_AT) {
            return null;
        }
        return lifetime >= SILVER_AT ? "Gold" : "Silver";
    }

    private int pointsToNextTier(int lifetime) {
        if (lifetime >= GOLD_AT) {
            return 0;
        }
        return (lifetime >= SILVER_AT ? GOLD_AT : SILVER_AT) - lifetime;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
