package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.LoyaltyTransactionRepository;
import com.bob.ecommerceangularapp.dto.LoyaltySummary;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for earning, redeeming and tiering loyalty points. */
class LoyaltyServiceTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final LoyaltyTransactionRepository ledger = mock(LoyaltyTransactionRepository.class);
    private final LoyaltyService service = new LoyaltyService(customerRepository, ledger);

    private Customer customer(String email, Integer balance, Integer lifetime) {
        Customer c = new Customer();
        c.setEmail(email);
        c.setLoyaltyPoints(balance);
        c.setLifetimePoints(lifetime);
        return c;
    }

    private Order order(String tracking, String total) {
        Order o = new Order();
        o.setId(1L);
        o.setOrderTrackingNumber(tracking);
        o.setTotalPrice(new BigDecimal(total));
        return o;
    }

    @Test
    void award_earnsOnePointPerDollarFloor() {
        Customer c = customer("a@b.com", null, null);
        int earned = service.award(c, order("T1", "42.99"));
        assertThat(earned).isEqualTo(42);
        assertThat(c.getLoyaltyPoints()).isEqualTo(42);
        assertThat(c.getLifetimePoints()).isEqualTo(42);
    }

    @Test
    void redeem_cappedByBalanceAndDrawsDown() {
        Customer c = customer("a@b.com", 100, 100);
        Order o = order("T2", "50.00"); // up to 5000 points worth
        BigDecimal discount = service.redeem(c, o, 250); // wants more than balance
        assertThat(discount).isEqualByComparingTo("1.00"); // 100 pts * $0.01
        assertThat(c.getLoyaltyPoints()).isZero();
        assertThat(o.getLoyaltyPointsRedeemed()).isEqualTo(100);
    }

    @Test
    void redeem_cappedByOrderTotal() {
        Customer c = customer("a@b.com", 1000, 1000);
        Order o = order("T3", "2.00"); // only 200 points worth
        BigDecimal discount = service.redeem(c, o, 1000);
        assertThat(discount).isEqualByComparingTo("2.00");
        assertThat(c.getLoyaltyPoints()).isEqualTo(800);
    }

    @Test
    void summary_computesTierAndProgress() {
        when(customerRepository.findByEmail("a@b.com")).thenReturn(customer("a@b.com", 300, 600));
        when(ledger.findTop20ByCustomerEmailIgnoreCaseOrderByDateCreatedDesc(any())).thenReturn(java.util.List.of());

        LoyaltySummary s = service.summary("a@b.com");

        assertThat(s.tier()).isEqualTo("Silver");      // 600 lifetime
        assertThat(s.nextTier()).isEqualTo("Gold");
        assertThat(s.pointsToNextTier()).isEqualTo(1400); // 2000 - 600
        assertThat(s.redeemableValue()).isEqualByComparingTo("3.00"); // 300 pts * $0.01
    }
}
