package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.TenantRepository;
import com.bob.ecommerceangularapp.dto.PlatformTenantRequest;
import com.bob.ecommerceangularapp.entity.Tenant;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB) for platform-level tenant CRUD (roadmap #21, Milestone B). */
class PlatformTenantServiceTest {

    private final TenantRepository repo = mock(TenantRepository.class);
    private final PlatformTenantService service = new PlatformTenantService(repo);

    private Tenant tenant(Long id, String slug) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setSlug(slug);
        t.setDisplayName(slug);
        t.setActive(true);
        return t;
    }

    @Test
    void save_createAssignsNoIdUpfrontAndPersists() {
        when(repo.existsBySlug("acme")).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });

        Tenant saved = service.save(new PlatformTenantRequest(null, "acme", "Acme Co", "hi@acme.test", "starter", true));

        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getSlug()).isEqualTo("acme");
        assertThat(saved.getPlan()).isEqualTo("starter");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void save_createRejectsADuplicateSlug() {
        when(repo.existsBySlug("demo")).thenReturn(true);

        assertThatThrownBy(() -> service.save(new PlatformTenantRequest(null, "demo", "Demo", null, null, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_updatePreservesIdAndAllowsAnUnchangedSlugOnSelfUpdate() {
        Tenant existing = tenant(3L, "acme");
        when(repo.existsBySlugAndIdNot("acme", 3L)).thenReturn(false);
        when(repo.findById(3L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant saved = service.save(new PlatformTenantRequest(3L, "acme", "Acme Renamed", null, null, true));

        assertThat(saved.getId()).isEqualTo(3L);
        assertThat(saved.getDisplayName()).isEqualTo("Acme Renamed");
    }

    @Test
    void save_updateRejectsASlugAlreadyUsedByADifferentTenant() {
        when(repo.existsBySlugAndIdNot("taken", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.save(new PlatformTenantRequest(3L, "taken", "Acme", null, null, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivate_setsActiveFalse() {
        Tenant existing = tenant(3L, "acme");
        when(repo.findById(3L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(3L);

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void deactivate_unknownIdThrows() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
