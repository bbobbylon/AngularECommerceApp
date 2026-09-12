package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ApiKeyRepository;
import com.bob.ecommerceangularapp.dto.AdminApiKeyRequest;
import com.bob.ecommerceangularapp.dto.ApiKeyCreatedView;
import com.bob.ecommerceangularapp.entity.ApiKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring/DB) for issue/list/revoke (roadmap #23, Milestone A). Needs
 * {@link TenantContext} scaffolding — every method reads it ambiently, mirroring
 * {@code GiftCardServiceTest}.
 */
class ApiKeyServiceTest {

    private static final Long TENANT_ID = 7L;

    private final ApiKeyRepository repo = mock(ApiKeyRepository.class);
    private final ApiKeyService service = new ApiKeyService(repo);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void issue_generatesAUniqueRawKeyNeverPersistedOnTheEntity() {
        when(repo.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ApiKeyCreatedView created = service.issue(new AdminApiKeyRequest("inventory-bot", List.of("OrderManager"), null));

        assertThat(created.rawKey()).isNotBlank();
        assertThat(created.keyPrefix()).isEqualTo(created.rawKey().substring(0, 16));

        var captor = org.mockito.ArgumentCaptor.forClass(ApiKey.class);
        verify(repo).save(captor.capture());
        ApiKey persisted = captor.getValue();
        assertThat(persisted.getKeyHash()).isEqualTo(ApiKeyLookupService.hash(created.rawKey()));
        assertThat(persisted.getKeyHash()).isNotEqualTo(created.rawKey());
        assertThat(persisted.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(persisted.getAuthorities()).isEqualTo("OrderManager");
        assertThat(persisted.isActive()).isTrue();
    }

    @Test
    void issue_joinsMultipleAuthoritiesWithCommas() {
        when(repo.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        service.issue(new AdminApiKeyRequest("full-access-bot", List.of("Admin", "OrderManager"), null));

        var captor = org.mockito.ArgumentCaptor.forClass(ApiKey.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAuthorities()).isEqualTo("Admin,OrderManager");
    }

    @Test
    void listAll_scopedToCurrentTenant() {
        ApiKey key = ApiKey.builder().id(1L).tenantId(TENANT_ID).build();
        when(repo.findAllByTenantId(TENANT_ID)).thenReturn(List.of(key));

        assertThat(service.listAll()).containsExactly(key);
    }

    @Test
    void revoke_setsInactiveAndRevokedAt() {
        ApiKey key = ApiKey.builder().id(1L).tenantId(TENANT_ID).active(true).build();
        when(repo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(key));
        when(repo.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        service.revoke(1L);

        assertThat(key.isActive()).isFalse();
        assertThat(key.getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_unknownIdThrows() {
        when(repo.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
