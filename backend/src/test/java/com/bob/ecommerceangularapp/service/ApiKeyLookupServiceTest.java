package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.ApiKeyRepository;
import com.bob.ecommerceangularapp.entity.ApiKey;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring/DB) for hash-based lookup + usability checks (roadmap #23, Milestone A).
 * No {@link com.bob.ecommerceangularapp.config.TenantContext} scaffolding needed here — this service
 * <i>produces</i> tenant identity from a key, it doesn't read the ambient context.
 */
class ApiKeyLookupServiceTest {

    private final ApiKeyRepository repo = mock(ApiKeyRepository.class);
    private final ApiKeyLookupService service = new ApiKeyLookupService(repo);

    private ApiKey usableKey() {
        return ApiKey.builder().id(1L).tenantId(5L).active(true).build();
    }

    @Test
    void lookup_hashesTheRawKeyBeforeQuerying() {
        String rawKey = "lsk_abc123";
        String expectedHash = ApiKeyLookupService.hash(rawKey);
        when(repo.findByKeyHash(expectedHash)).thenReturn(Optional.of(usableKey()));

        Optional<ApiKey> found = service.lookup(rawKey);

        assertThat(found).isPresent();
        assertThat(found.get().getTenantId()).isEqualTo(5L);
    }

    @Test
    void lookup_noMatchReturnsEmpty() {
        when(repo.findByKeyHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(service.lookup("unknown-key")).isEmpty();
    }

    @Test
    void isUsable_rejectsInactive() {
        ApiKey key = usableKey();
        key.setActive(false);
        assertThat(service.isUsable(key)).isFalse();
    }

    @Test
    void isUsable_rejectsRevoked() {
        ApiKey key = usableKey();
        key.setRevokedAt(new Date());
        assertThat(service.isUsable(key)).isFalse();
    }

    @Test
    void isUsable_rejectsExpired() {
        ApiKey key = usableKey();
        key.setExpiresAt(new Date(System.currentTimeMillis() - 60_000));
        assertThat(service.isUsable(key)).isFalse();
    }

    @Test
    void isUsable_acceptsActiveNonExpiredNonRevoked() {
        ApiKey key = usableKey();
        key.setExpiresAt(new Date(System.currentTimeMillis() + 60_000));
        assertThat(service.isUsable(key)).isTrue();
    }

    @Test
    void isUsable_acceptsNullExpiryAsNeverExpiring() {
        assertThat(service.isUsable(usableKey())).isTrue();
    }

    @Test
    void hash_isDeterministicAndDependsOnInput() {
        assertThat(ApiKeyLookupService.hash("same")).isEqualTo(ApiKeyLookupService.hash("same"));
        assertThat(ApiKeyLookupService.hash("a")).isNotEqualTo(ApiKeyLookupService.hash("b"));
    }
}
