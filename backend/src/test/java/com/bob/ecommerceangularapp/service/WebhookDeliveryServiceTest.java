package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.WebhookDeliveryAttemptRepository;
import com.bob.ecommerceangularapp.dao.WebhookEventRepository;
import com.bob.ecommerceangularapp.dao.WebhookSubscriptionRepository;
import com.bob.ecommerceangularapp.entity.WebhookDeliveryAttempt;
import com.bob.ecommerceangularapp.entity.WebhookEvent;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring/DB, no real HTTP) for webhook delivery/retry/signing (roadmap #23,
 * Milestone D). Mirrors {@code BillingServiceTest}'s style: every branch of a delivery attempt
 * writes a ledger row and returns normally. {@link WebhookDeliveryService#sendRequest} — the one
 * call that reaches the network — is stubbed via a spy rather than mocking {@link RestClient}'s
 * heavily self-referential generic builder chain, which is fragile with deep stubs; this isolates
 * exactly the seam that's actually external, the same way {@code BillingServiceTest} never exercises
 * Stripe's own static SDK calls.
 */
class WebhookDeliveryServiceTest {

    private static final Long TENANT_ID = 7L;

    private final WebhookEventRepository eventRepository = mock(WebhookEventRepository.class);
    private final WebhookSubscriptionRepository subscriptionRepository = mock(WebhookSubscriptionRepository.class);
    private final WebhookDeliveryAttemptRepository attemptRepository = mock(WebhookDeliveryAttemptRepository.class);
    private final RestClient restClient = mock(RestClient.class);
    private final WebhookDeliveryService service =
            spy(new WebhookDeliveryService(eventRepository, subscriptionRepository, attemptRepository, restClient));

    private static WebhookSubscription subscription() {
        return WebhookSubscription.builder().id(1L).tenantId(TENANT_ID)
                .url("https://example.com/hook").secret("whsec_test").eventTypes("order.created").active(true).build();
    }

    private static WebhookEvent event(int attemptCount, String status) {
        return WebhookEvent.builder().id(100L).tenantId(TENANT_ID).webhookSubscriptionId(1L)
                .eventType("order.created").payload("{\"orderId\":1}").status(status)
                .attemptCount(attemptCount).build();
    }

    // ---- sign(): a concrete, independently assertable computation ----

    @Test
    void sign_isDeterministicForTheSameSecretAndPayload() {
        assertThat(WebhookDeliveryService.sign("whsec_a", "{}")).isEqualTo(WebhookDeliveryService.sign("whsec_a", "{}"));
    }

    @Test
    void sign_dependsOnBothSecretAndPayload() {
        assertThat(WebhookDeliveryService.sign("whsec_a", "{}")).isNotEqualTo(WebhookDeliveryService.sign("whsec_b", "{}"));
        assertThat(WebhookDeliveryService.sign("whsec_a", "{\"x\":1}")).isNotEqualTo(WebhookDeliveryService.sign("whsec_a", "{\"y\":1}"));
    }

    @Test
    void sign_matchesAnIndependentlyComputedHmacSha256() throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("whsec_test".getBytes(), "HmacSHA256"));
        String expected = java.util.HexFormat.of().formatHex(mac.doFinal("{\"orderId\":1}".getBytes()));

        assertThat(WebhookDeliveryService.sign("whsec_test", "{\"orderId\":1}")).isEqualTo(expected);
    }

    // ---- deliverOne(): retry/backoff/abandon state machine ----

    @Test
    void deliverOne_successRecordsSucceededAttemptAndMarksDelivered() {
        WebhookSubscription sub = subscription();
        WebhookEvent evt = event(0, "PENDING");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        doReturn(ResponseEntity.ok().build()).when(service).sendRequest(sub, evt);

        service.deliverOne(evt);

        ArgumentCaptor<WebhookDeliveryAttempt> captor = ArgumentCaptor.forClass(WebhookDeliveryAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo("SUCCEEDED");
        assertThat(captor.getValue().getHttpStatusCode()).isEqualTo(200);
        assertThat(captor.getValue().getEventType()).isEqualTo("order.created");
        assertThat(evt.getStatus()).isEqualTo("DELIVERED");
        assertThat(evt.getAttemptCount()).isEqualTo(1);
        assertThat(evt.getNextAttemptAt()).isNull();
    }

    @Test
    void deliverOne_nonSuccessResponseAdvancesBackoffAndStaysFailed() {
        WebhookSubscription sub = subscription();
        WebhookEvent evt = event(0, "PENDING");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        doThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null))
                .when(service).sendRequest(sub, evt);

        service.deliverOne(evt);

        verify(attemptRepository).save(argThat(a -> "FAILED".equals(a.getOutcome()) && Integer.valueOf(500).equals(a.getHttpStatusCode())));
        assertThat(evt.getStatus()).isEqualTo("FAILED");
        assertThat(evt.getAttemptCount()).isEqualTo(1);
        assertThat(evt.getNextAttemptAt()).isNotNull();
    }

    @Test
    void deliverOne_networkErrorRecordsFailedAttemptWithNoStatusCode() {
        WebhookSubscription sub = subscription();
        WebhookEvent evt = event(0, "PENDING");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        doThrow(new RuntimeException("Connection refused")).when(service).sendRequest(sub, evt);

        service.deliverOne(evt);

        verify(attemptRepository).save(argThat(a -> "FAILED".equals(a.getOutcome()) && a.getHttpStatusCode() == null));
        assertThat(evt.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void deliverOne_abandonsOnceAttemptsAreExhausted() {
        WebhookSubscription sub = subscription();
        WebhookEvent evt = event(4, "FAILED"); // this will be the 5th (final) attempt
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        doThrow(new RuntimeException("still down")).when(service).sendRequest(sub, evt);

        service.deliverOne(evt);

        assertThat(evt.getStatus()).isEqualTo("ABANDONED");
        assertThat(evt.getAttemptCount()).isEqualTo(5);
        assertThat(evt.getNextAttemptAt()).isNull();
    }

    @Test
    void deliverOne_deactivatedSubscriptionAbandonsImmediatelyWithNoAttempt() {
        WebhookSubscription inactive = subscription();
        inactive.setActive(false);
        WebhookEvent evt = event(0, "PENDING");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(inactive));

        service.deliverOne(evt);

        assertThat(evt.getStatus()).isEqualTo("ABANDONED");
        verify(attemptRepository, never()).save(any());
        verify(eventRepository).save(evt);
    }

    @Test
    void deliverDue_oneFailingEventNeverStopsTheRestFromBeingProcessed() {
        WebhookEvent failing = event(0, "PENDING");
        failing.setId(1L);
        failing.setWebhookSubscriptionId(1L);
        WebhookEvent succeeding = event(0, "PENDING");
        succeeding.setId(2L);
        succeeding.setWebhookSubscriptionId(2L);
        WebhookSubscription sub1 = subscription();
        sub1.setId(1L);
        WebhookSubscription sub2 = subscription();
        sub2.setId(2L);

        when(eventRepository.findAllByStatusInAndNextAttemptAtLessThanEqual(anyList(), any())).thenReturn(List.of(failing, succeeding));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub1));
        when(subscriptionRepository.findById(2L)).thenReturn(Optional.of(sub2));
        doThrow(new RuntimeException("down")).when(service).sendRequest(sub1, failing);
        doReturn(ResponseEntity.ok().build()).when(service).sendRequest(sub2, succeeding);

        int count = service.deliverDue();

        assertThat(count).isEqualTo(2);
        assertThat(failing.getStatus()).isEqualTo("FAILED");
        assertThat(succeeding.getStatus()).isEqualTo("DELIVERED");
        verify(attemptRepository, times(2)).save(any());
    }
}
