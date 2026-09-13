package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.WebhookDeliveryAttemptRepository;
import com.bob.ecommerceangularapp.dao.WebhookSubscriptionRepository;
import com.bob.ecommerceangularapp.dto.AdminWebhookSubscriptionRequest;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring/DB) for webhook subscription CRUD (roadmap #23, Milestone B). Needs
 * {@link TenantContext} scaffolding, mirroring {@code GiftCardServiceTest}.
 */
class WebhookSubscriptionServiceTest {

    private static final Long TENANT_ID = 7L;

    private final WebhookSubscriptionRepository repo = mock(WebhookSubscriptionRepository.class);
    private final WebhookDeliveryAttemptRepository deliveryAttemptRepo = mock(WebhookDeliveryAttemptRepository.class);
    private final WebhookSubscriptionService service = new WebhookSubscriptionService(repo, deliveryAttemptRepo);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void save_createsWithAFreshGeneratedSecretWhenIdIsNull() {
        when(repo.save(any(WebhookSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookSubscription saved = service.save(new AdminWebhookSubscriptionRequest(
                null, "https://example.com/hook", List.of("order.created"), true));

        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getSecret()).startsWith("whsec_");
        assertThat(saved.getEventTypes()).isEqualTo("order.created");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void save_generatesDifferentSecretsForDifferentSubscriptions() {
        when(repo.save(any(WebhookSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookSubscription first = service.save(new AdminWebhookSubscriptionRequest(
                null, "https://example.com/a", List.of("order.created"), true));
        WebhookSubscription second = service.save(new AdminWebhookSubscriptionRequest(
                null, "https://example.com/b", List.of("order.created"), true));

        assertThat(first.getSecret()).isNotEqualTo(second.getSecret());
    }

    @Test
    void save_updateReusesExistingSecretAndNeverRegeneratesIt() {
        WebhookSubscription existing = WebhookSubscription.builder()
                .id(1L).tenantId(TENANT_ID).secret("whsec_original").url("https://old.example.com")
                .eventTypes("order.created").active(true).build();
        when(repo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(repo.save(any(WebhookSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookSubscription updated = service.save(new AdminWebhookSubscriptionRequest(
                1L, "https://new.example.com", List.of("order.created", "return.approved"), false));

        assertThat(updated.getSecret()).isEqualTo("whsec_original");
        assertThat(updated.getUrl()).isEqualTo("https://new.example.com");
        assertThat(updated.getEventTypes()).isEqualTo("order.created,return.approved");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void save_updateUnknownIdThrows() {
        when(repo.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.save(new AdminWebhookSubscriptionRequest(
                99L, "https://example.com", List.of("order.created"), true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listAll_scopedToCurrentTenant() {
        WebhookSubscription sub = WebhookSubscription.builder().id(1L).tenantId(TENANT_ID).build();
        when(repo.findAllByTenantId(TENANT_ID)).thenReturn(List.of(sub));

        assertThat(service.listAll()).containsExactly(sub);
    }

    @Test
    void deactivate_setsInactiveWithoutDeleting() {
        WebhookSubscription sub = WebhookSubscription.builder().id(1L).tenantId(TENANT_ID).active(true).build();
        when(repo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(sub));
        when(repo.save(any(WebhookSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(1L);

        assertThat(sub.isActive()).isFalse();
    }

    @Test
    void deactivate_unknownIdThrows() {
        when(repo.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivate(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
