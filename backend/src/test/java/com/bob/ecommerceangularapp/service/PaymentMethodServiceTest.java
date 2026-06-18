package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.SavedPaymentMethodRepository;
import com.bob.ecommerceangularapp.dto.SetupIntentResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Pure unit tests for the graceful-degradation behaviour when Stripe isn't configured. */
class PaymentMethodServiceTest {

    private final SavedPaymentMethodRepository repo = mock(SavedPaymentMethodRepository.class);
    // empty key = Stripe not configured
    private final PaymentMethodService service = new PaymentMethodService(repo, "");

    @Test
    void setupIntent_disabledWithoutStripe() {
        SetupIntentResponse response = service.createSetupIntent("a@b.com");
        assertThat(response.enabled()).isFalse();
        assertThat(response.clientSecret()).isNull();
    }

    @Test
    void record_throwsWithoutStripe() {
        assertThatThrownBy(() -> service.record("a@b.com", "pm_123"))
                .isInstanceOf(IllegalStateException.class);
    }
}
