package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.FaqEntryRepository;
import com.bob.ecommerceangularapp.dao.SiteBannerRepository;
import com.bob.ecommerceangularapp.dto.FaqEntryRequest;
import com.bob.ecommerceangularapp.dto.SiteBannerRequest;
import com.bob.ecommerceangularapp.entity.FaqEntry;
import com.bob.ecommerceangularapp.entity.SiteBanner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit test (mocked repositories) for the simple CMS: the singleton banner + the FAQ list. */
class ContentServiceTest {

    private final SiteBannerRepository siteBannerRepository = mock(SiteBannerRepository.class);
    private final FaqEntryRepository faqEntryRepository = mock(FaqEntryRepository.class);
    private final ContentService service = new ContentService(siteBannerRepository, faqEntryRepository);

    @Test
    void noBannerConfigured_yieldsEmptyActiveBanner() {
        when(siteBannerRepository.findAll()).thenReturn(List.of());
        assertThat(service.getActiveBanner()).isEmpty();
    }

    @Test
    void inactiveBanner_isNotReturnedToTheStorefront() {
        SiteBanner banner = new SiteBanner();
        banner.setMessage("Sale!");
        banner.setActive(false);
        when(siteBannerRepository.findAll()).thenReturn(List.of(banner));
        assertThat(service.getActiveBanner()).isEmpty();
    }

    @Test
    void activeBanner_isReturnedToTheStorefront() {
        SiteBanner banner = new SiteBanner();
        banner.setMessage("Sale!");
        banner.setActive(true);
        when(siteBannerRepository.findAll()).thenReturn(List.of(banner));
        assertThat(service.getActiveBanner()).contains(banner);
    }

    @Test
    void getBannerForAdmin_returnsABlankShellWhenNoneExists() {
        when(siteBannerRepository.findAll()).thenReturn(List.of());
        SiteBanner banner = service.getBannerForAdmin();
        assertThat(banner.getId()).isNull();
    }

    @Test
    void saveBanner_createsWhenNoneExistsYet() {
        when(siteBannerRepository.findAll()).thenReturn(List.of());
        when(siteBannerRepository.save(any(SiteBanner.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteBanner saved = service.saveBanner(new SiteBannerRequest("Big sale", "/sale", "Shop now", true));

        assertThat(saved.getMessage()).isEqualTo("Big sale");
        assertThat(saved.getLinkUrl()).isEqualTo("/sale");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void saveBanner_updatesTheExistingSingletonRowInPlace() {
        SiteBanner existing = new SiteBanner();
        existing.setId(1L);
        existing.setMessage("Old message");
        when(siteBannerRepository.findAll()).thenReturn(List.of(existing));
        when(siteBannerRepository.save(any(SiteBanner.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteBanner saved = service.saveBanner(new SiteBannerRequest("New message", null, null, false));

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getMessage()).isEqualTo("New message");
        assertThat(saved.getLinkUrl()).isNull();
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void listActiveFaq_delegatesToTheOrderedRepositoryQuery() {
        FaqEntry entry = new FaqEntry();
        when(faqEntryRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(entry));
        assertThat(service.listActiveFaq()).containsExactly(entry);
    }

    @Test
    void saveFaq_createsWhenIdIsAbsent() {
        when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        FaqEntry saved = service.saveFaq(new FaqEntryRequest(null, "Q?", "A.", 1, true));

        assertThat(saved.getQuestion()).isEqualTo("Q?");
        assertThat(saved.getAnswer()).isEqualTo("A.");
    }

    @Test
    void saveFaq_updatesInPlaceWhenIdIsPresent() {
        FaqEntry existing = new FaqEntry();
        existing.setId(5L);
        when(faqEntryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        FaqEntry saved = service.saveFaq(new FaqEntryRequest(5L, "Updated?", "Updated.", 2, false));

        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getQuestion()).isEqualTo("Updated?");
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void saveFaq_unknownId_throws() {
        when(faqEntryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.saveFaq(new FaqEntryRequest(99L, "Q?", "A.", 1, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteFaq_removesAnExistingEntry() {
        when(faqEntryRepository.existsById(1L)).thenReturn(true);
        service.deleteFaq(1L);
        verify(faqEntryRepository).deleteById(1L);
    }

    @Test
    void deleteFaq_unknownId_throws() {
        when(faqEntryRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteFaq(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
