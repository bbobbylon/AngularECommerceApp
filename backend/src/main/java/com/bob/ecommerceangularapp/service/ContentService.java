package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.FaqEntryRepository;
import com.bob.ecommerceangularapp.dao.SiteBannerRepository;
import com.bob.ecommerceangularapp.dto.FaqEntryRequest;
import com.bob.ecommerceangularapp.dto.SiteBannerRequest;
import com.bob.ecommerceangularapp.entity.FaqEntry;
import com.bob.ecommerceangularapp.entity.SiteBanner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * A deliberately small CMS (roadmap #17): a single site-wide announcement banner and a list of FAQ
 * entries, both previously hardcoded in the frontend, now editable by an admin without a deploy.
 */
@Service
public class ContentService {

    private final SiteBannerRepository siteBannerRepository;
    private final FaqEntryRepository faqEntryRepository;

    public ContentService(SiteBannerRepository siteBannerRepository, FaqEntryRepository faqEntryRepository) {
        this.siteBannerRepository = siteBannerRepository;
        this.faqEntryRepository = faqEntryRepository;
    }

    // ----- storefront -----

    /** The banner, if one is configured and turned on. */
    @Transactional(readOnly = true)
    public Optional<SiteBanner> getActiveBanner() {
        return currentBanner().filter(SiteBanner::isActive);
    }

    @Transactional(readOnly = true)
    public List<FaqEntry> listActiveFaq() {
        return faqEntryRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
    }

    // ----- admin -----

    /** The banner row for editing, even if currently turned off; a blank shell if none exists yet. */
    @Transactional(readOnly = true)
    public SiteBanner getBannerForAdmin() {
        return currentBanner().orElseGet(SiteBanner::new);
    }

    @Transactional
    public SiteBanner saveBanner(SiteBannerRequest request) {
        SiteBanner banner = currentBanner().orElseGet(SiteBanner::new);
        banner.setMessage(request.message().trim());
        banner.setLinkUrl(blankToNull(request.linkUrl()));
        banner.setLinkText(blankToNull(request.linkText()));
        banner.setActive(request.active());
        return siteBannerRepository.save(banner);
    }

    @Transactional(readOnly = true)
    public List<FaqEntry> listFaqForAdmin() {
        return faqEntryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public FaqEntry saveFaq(FaqEntryRequest request) {
        FaqEntry entry = request.id() != null
                ? faqEntryRepository.findById(request.id())
                        .orElseThrow(() -> new IllegalArgumentException("FAQ entry not found: " + request.id()))
                : new FaqEntry();
        entry.setQuestion(request.question().trim());
        entry.setAnswer(request.answer().trim());
        entry.setSortOrder(request.sortOrder());
        entry.setActive(request.active());
        return faqEntryRepository.save(entry);
    }

    @Transactional
    public void deleteFaq(Long id) {
        if (!faqEntryRepository.existsById(id)) {
            throw new IllegalArgumentException("FAQ entry not found: " + id);
        }
        faqEntryRepository.deleteById(id);
    }

    private Optional<SiteBanner> currentBanner() {
        return siteBannerRepository.findAll().stream().findFirst();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
