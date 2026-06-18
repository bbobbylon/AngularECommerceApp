package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.GiftCardRepository;
import com.bob.ecommerceangularapp.dto.GiftCardView;
import com.bob.ecommerceangularapp.entity.GiftCard;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for gift-card check + redemption. */
class GiftCardServiceTest {

    private final GiftCardRepository repo = mock(GiftCardRepository.class);
    private final GiftCardService service = new GiftCardService(repo);

    private GiftCard card(String code, String balance, boolean active) {
        return GiftCard.builder().code(code).balance(new BigDecimal(balance))
                .initialBalance(new BigDecimal(balance)).active(active).build();
    }

    @Test
    void check_returnsBalanceForActiveCard() {
        when(repo.findByCodeIgnoreCase("GIFT25")).thenReturn(Optional.of(card("GIFT25", "25.00", true)));
        GiftCardView view = service.check("GIFT25");
        assertThat(view.valid()).isTrue();
        assertThat(view.balance()).isEqualByComparingTo("25.00");
    }

    @Test
    void check_rejectsInactiveOrEmpty() {
        when(repo.findByCodeIgnoreCase("DEAD")).thenReturn(Optional.of(card("DEAD", "10.00", false)));
        assertThat(service.check("DEAD").valid()).isFalse();
        when(repo.findByCodeIgnoreCase("ZERO")).thenReturn(Optional.of(card("ZERO", "0.00", true)));
        assertThat(service.check("ZERO").valid()).isFalse();
    }

    @Test
    void redeem_appliesUpToBalanceAndDrawsDown() {
        GiftCard c = card("GIFT25", "25.00", true);
        when(repo.findByCodeIgnoreCase("GIFT25")).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal applied = service.redeem("GIFT25", new BigDecimal("40.00")); // order bigger than card

        assertThat(applied).isEqualByComparingTo("25.00"); // capped at balance
        assertThat(c.getBalance()).isEqualByComparingTo("0.00"); // fully drawn down
    }

    @Test
    void redeem_partialLeavesRemainingBalance() {
        GiftCard c = card("GIFT50", "50.00", true);
        when(repo.findByCodeIgnoreCase("GIFT50")).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal applied = service.redeem("GIFT50", new BigDecimal("30.00")); // order smaller than card

        assertThat(applied).isEqualByComparingTo("30.00");
        assertThat(c.getBalance()).isEqualByComparingTo("20.00");
    }

    @Test
    void redeem_unknownCodeAppliesNothing() {
        when(repo.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());
        assertThat(service.redeem("NOPE", new BigDecimal("10.00"))).isEqualByComparingTo("0");
    }
}
