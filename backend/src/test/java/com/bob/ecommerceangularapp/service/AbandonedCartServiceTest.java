package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.AbandonedCartRepository;
import com.bob.ecommerceangularapp.dto.AbandonedCartRequest;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.AbandonedCart;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for capture / recover / remind. */
class AbandonedCartServiceTest {

    private final AbandonedCartRepository repo = mock(AbandonedCartRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final AbandonedCartService service = new AbandonedCartService(repo, emailService, 60);

    @Test
    void capture_savesSnapshotForNewEmail() {
        when(repo.findFirstByEmailIgnoreCaseAndRecoveredFalseOrderByIdDesc("a@b.com"))
                .thenReturn(Optional.empty());
        service.capture(new AbandonedCartRequest("a@b.com", 2, new BigDecimal("30.00"), "2 items"));
        verify(repo).save(any(AbandonedCart.class));
    }

    @Test
    void capture_skipsEmptyCart() {
        service.capture(new AbandonedCartRequest("a@b.com", 0, BigDecimal.ZERO, ""));
        verify(repo, never()).save(any());
    }

    @Test
    void markRecovered_flagsAllLiveCarts() {
        AbandonedCart c = new AbandonedCart();
        when(repo.findByEmailIgnoreCaseAndRecoveredFalse("a@b.com")).thenReturn(List.of(c));
        service.markRecovered("a@b.com");
        assertThat(c.isRecovered()).isTrue();
        verify(repo).saveAll(any());
    }

    @Test
    void remindStale_emailsAndMarksReminded() {
        AbandonedCart c1 = new AbandonedCart();
        c1.setEmail("a@b.com");
        c1.setItemCount(1);
        c1.setTotal(new BigDecimal("10.00"));
        AbandonedCart c2 = new AbandonedCart();
        c2.setEmail("c@d.com");
        c2.setItemCount(3);
        c2.setTotal(new BigDecimal("99.00"));
        when(repo.findByRecoveredFalseAndRemindedFalseAndLastUpdatedBefore(any(Date.class)))
                .thenReturn(List.of(c1, c2));

        int sent = service.remindStale();

        assertThat(sent).isEqualTo(2);
        verify(emailService, times(2)).sendAbandonedCart(anyString(), anyInt(), any(BigDecimal.class));
        assertThat(c1.isReminded()).isTrue();
        assertThat(c2.isReminded()).isTrue();
    }
}
