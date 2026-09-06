package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.FaqEntryRepository;
import com.bob.ecommerceangularapp.dao.SiteBannerRepository;
import com.bob.ecommerceangularapp.dto.FaqEntryRequest;
import com.bob.ecommerceangularapp.dto.SiteBannerRequest;
import com.bob.ecommerceangularapp.entity.FaqEntry;
import com.bob.ecommerceangularapp.entity.SiteBanner;
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
 * Pure unit test (mocked repositories) for the simple CMS: the per-tenant banner + FAQ list, and
 * (roadmap #21, Milestone C) that every read/write is scoped to {@link TenantContext}.
 */
class ContentServiceTest {

    private static final Long TENANT_ID = 7L;

    private final SiteBannerRepository siteBannerRepository = mock(SiteBannerRepository.class);
    private final FaqEntryRepository faqEntryRepository = mock(FaqEntryRepository.class);
    private final ContentService service = new ContentService(siteBannerRepository, faqEntryRepository);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void noBannerConfigured_yieldsEmptyActiveBanner() {
        when(siteBannerRepository.findFirstByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        assertThat(service.getActiveBanner()).isEmpty();
    }

    @Test
    void inactiveBanner_isNotReturnedToTheStorefront() {
        SiteBanner banner = new SiteBanner();
        banner.setMessage("Sale!");
        banner.setActive(false);
        when(siteBannerRepository.findFirstByTenantId(TENANT_ID)).thenReturn(Optional.of(banner));
        assertThat(service.getActiveBanner()).isEmpty();
    }

    @Test
    void activeBanner_isReturnedToTheStorefront() {
        SiteBanner banner = new SiteBanner();
        banner.setMessage("Sale!");
        banner.setActive(true);
        when(siteBannerRepository.findFirstByTenantId(TENANT_ID)).thenReturn(Optional.of(banner));
        assertThat(service.getActiveBanner()).contains(banner);
    }

    @Test
    void getBannerForAdmin_returnsABlankShellWhenNoneExists() {
        when(siteBannerRepository.findFirstByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        SiteBanner banner = service.getBannerForAdmin();
        assertThat(banner.getId()).isNull();
    }

    @Test
    void saveBanner_createsWhenNoneExistsYet_andStampsCurrentTenant() {
        when(siteBannerRepository.findFirstByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(siteBannerRepository.save(any(SiteBanner.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteBanner saved = service.saveBanner(new SiteBannerRequest("Big sale", "/sale", "Shop now", true));

        assertThat(saved.getMessage()).isEqualTo("Big sale");
        assertThat(saved.getLinkUrl()).isEqualTo("/sale");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void saveBanner_updatesTheExistingSingletonRowInPlace() {
        SiteBanner existing = new SiteBanner();
        existing.setId(1L);
        existing.setTenantId(TENANT_ID);
        existing.setMessage("Old message");
        when(siteBannerRepository.findFirstByTenantId(TENANT_ID)).thenReturn(Optional.of(existing));
        when(siteBannerRepository.save(any(SiteBanner.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteBanner saved = service.saveBanner(new SiteBannerRequest("New message", null, null, false));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getMessage()).isEqualTo("New message");
        assertThat(saved.getLinkUrl()).isNull();
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void listActiveFaq_delegatesToTheTenantScopedOrderedRepositoryQuery() {
        FaqEntry entry = new FaqEntry();
        when(faqEntryRepository.findByActiveTrueAndTenantIdOrderBySortOrderAscIdAsc(TENANT_ID)).thenReturn(List.of(entry));
        assertThat(service.listActiveFaq()).containsExactly(entry);
    }

    @Test
    void saveFaq_createsWhenIdIsAbsent_andStampsCurrentTenant() {
        when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        FaqEntry saved = service.saveFaq(new FaqEntryRequest(null, "Q?", "A.", 1, true));

        assertThat(saved.getQuestion()).isEqualTo("Q?");
        assertThat(saved.getAnswer()).isEqualTo("A.");
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void saveFaq_updatesInPlaceWhenIdIsPresent() {
        FaqEntry existing = new FaqEntry();
        existing.setId(5L);
        when(faqEntryRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        FaqEntry saved = service.saveFaq(new FaqEntryRequest(5L, "Updated?", "Updated.", 2, false));

        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getQuestion()).isEqualTo("Updated?");
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void saveFaq_unknownId_throws() {
        when(faqEntryRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.saveFaq(new FaqEntryRequest(99L, "Q?", "A.", 1, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteFaq_removesAnExistingEntry() {
        FaqEntry existing = new FaqEntry();
        existing.setId(1L);
        when(faqEntryRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        service.deleteFaq(1L);
        verify(faqEntryRepository).delete(existing);
    }

    @Test
    void deleteFaq_unknownId_throws() {
        when(faqEntryRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteFaq(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
