package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.CustomerRepository;
import com.bob.ecommerceangularapp.dao.ReferralRepository;
import com.bob.ecommerceangularapp.dto.ReferralSummary;
import com.bob.ecommerceangularapp.entity.Customer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for the referral lifecycle. */
class ReferralServiceTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final ReferralRepository referralRepository = mock(ReferralRepository.class);
    private final LoyaltyService loyaltyService = mock(LoyaltyService.class);
    private final ReferralService service =
            new ReferralService(customerRepository, referralRepository, loyaltyService);

    private Customer customer(String email, String code) {
        Customer c = new Customer();
        c.setEmail(email);
        c.setReferralCode(code);
        return c;
    }

    @Test
    void recordReferral_rewardsBothPartiesOnFirstQualifyingOrder() {
        Customer referee = customer("new@x.com", null);
        when(referralRepository.existsByRefereeEmailIgnoreCase("new@x.com")).thenReturn(false);
        when(customerRepository.findByReferralCode("REFABC123")).thenReturn(customer("old@x.com", "REFABC123"));

        service.recordReferral(referee, "refabc123", 99L);

        verify(referralRepository).save(any());
        verify(loyaltyService, times(2)).grantPoints(any(), anyInt(), anyString()); // referrer + referee
    }

    @Test
    void recordReferral_skipsSelfReferral() {
        Customer referee = customer("me@x.com", "REFSELF");
        when(referralRepository.existsByRefereeEmailIgnoreCase("me@x.com")).thenReturn(false);
        when(customerRepository.findByReferralCode("REFSELF")).thenReturn(customer("me@x.com", "REFSELF"));

        service.recordReferral(referee, "REFSELF", 1L);

        verify(referralRepository, never()).save(any());
        verify(loyaltyService, never()).grantPoints(any(), anyInt(), anyString());
    }

    @Test
    void recordReferral_skipsAlreadyReferred() {
        when(referralRepository.existsByRefereeEmailIgnoreCase("new@x.com")).thenReturn(true);
        service.recordReferral(customer("new@x.com", null), "REFABC123", 1L);
        verify(referralRepository, never()).save(any());
    }

    @Test
    void summary_assignsCodeForKnownCustomer() {
        when(customerRepository.findByEmail("a@b.com")).thenReturn(customer("a@b.com", null));
        when(customerRepository.existsByReferralCode(anyString())).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(referralRepository.findByReferrerCode(anyString())).thenReturn(List.of());

        ReferralSummary s = service.summary("a@b.com");

        assertThat(s.code()).startsWith("REF");
        assertThat(s.referrerReward()).isEqualTo(ReferralService.REFERRER_REWARD);
    }
}
