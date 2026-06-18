package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.StockNotificationRepository;
import com.bob.ecommerceangularapp.dto.StockNotificationRequest;
import com.bob.ecommerceangularapp.email.EmailService;
import com.bob.ecommerceangularapp.entity.Product;
import com.bob.ecommerceangularapp.entity.StockNotification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for back-in-stock subscribe + notify. */
class StockNotificationServiceTest {

    private final StockNotificationRepository repo = mock(StockNotificationRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final StockNotificationService service = new StockNotificationService(repo, emailService);

    private Product product(long id, String name, int stock) {
        Product p = Product.builder().id(id).name(name).unitsInStock(stock).build();
        return p;
    }

    @Test
    void subscribe_savesWhenNotAlreadyWaiting() {
        when(repo.existsByEmailIgnoreCaseAndProductIdAndVariantSkuAndNotifiedFalse("a@b.com", 1L, null))
                .thenReturn(false);
        service.subscribe(new StockNotificationRequest("a@b.com", 1L, null));
        verify(repo).save(any(StockNotification.class));
    }

    @Test
    void subscribe_skipsDuplicate() {
        when(repo.existsByEmailIgnoreCaseAndProductIdAndVariantSkuAndNotifiedFalse("a@b.com", 1L, null))
                .thenReturn(true);
        service.subscribe(new StockNotificationRequest("a@b.com", 1L, null));
        verify(repo, never()).save(any());
    }

    @Test
    void notifyProductRestocked_emailsWaitersAndMarksNotified() {
        StockNotification n1 = new StockNotification();
        n1.setEmail("a@b.com");
        StockNotification n2 = new StockNotification();
        n2.setEmail("c@d.com");
        when(repo.findByProductIdAndVariantSkuIsNullAndNotifiedFalse(1L)).thenReturn(List.of(n1, n2));

        service.notifyProductRestocked(product(1L, "Mug", 5));

        verify(emailService, times(2)).sendBackInStock(anyString(), eq("Mug"), eq(1L));
        verify(repo).saveAll(any());
        assertThat(n1.isNotified()).isTrue();
        assertThat(n2.isNotified()).isTrue();
    }

    @Test
    void notifyProductRestocked_noOpWhenStillOutOfStock() {
        service.notifyProductRestocked(product(1L, "Mug", 0));
        verify(repo, never()).findByProductIdAndVariantSkuIsNullAndNotifiedFalse(anyLong());
        verify(emailService, never()).sendBackInStock(anyString(), anyString(), anyLong());
    }
}
