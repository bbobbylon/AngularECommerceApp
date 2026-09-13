package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ConsentRecordRepository;
import com.bob.ecommerceangularapp.dto.ConsentRequest;
import com.bob.ecommerceangularapp.dto.ConsentView;
import com.bob.ecommerceangularapp.entity.ConsentRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Records and reads cookie/storage consent decisions (roadmap #24).
 *
 * <p>Writes are append-only — {@link #record} always inserts, never updates. The visitor's current
 * choice is defined as their most recent row, so changing or withdrawing consent leaves the earlier
 * decision intact and provable, which is what GDPR Art. 7(1) actually asks for. A mutable
 * current-state row would make "we had consent on the 3rd" unfalsifiable.
 *
 * <p>Enforcement of a consent decision lives in the browser, not here: the categories govern
 * {@code localStorage} keys the Angular app writes, so the frontend's {@code ConsentService} is the
 * thing that must honour them. This service is the durable record of what was chosen — the two are
 * deliberately separate, because a server-side ledger cannot stop a client-side write and a
 * client-side flag cannot prove anything after the fact.
 */
@Service
public class PrivacyConsentService {

    private final ConsentRecordRepository consentRecordRepository;
    private final String policyVersion;

    public PrivacyConsentService(ConsentRecordRepository consentRecordRepository,
                                 @Value("${app.privacy.policy-version:2026-09-01}") String policyVersion) {
        this.consentRecordRepository = consentRecordRepository;
        this.policyVersion = policyVersion;
    }

    /** The policy version currently in force; the storefront re-prompts when this changes. */
    public String policyVersion() {
        return policyVersion;
    }

    /**
     * Appends one decision. {@code necessary} is forced true rather than read from the request —
     * strictly-necessary storage (the cart, and the consent choice itself) is not on offer, and
     * accepting it as input would imply a choice the visitor does not have.
     */
    @Transactional
    public ConsentView record(ConsentRequest request) {
        ConsentRecord record = ConsentRecord.builder()
                .tenantId(TenantContext.currentTenantId())
                .visitorId(request.visitorId().trim())
                .email(normalizeOrNull(request.email()))
                .necessary(true)
                .functional(request.functional())
                .analytics(request.analytics())
                .marketing(request.marketing())
                .policyVersion(policyVersion)
                .source(resolveSource(request.source()))
                .build();
        return ConsentView.from(consentRecordRepository.save(record));
    }

    /** The visitor's current choice — i.e. their latest row, or empty if they have never chosen. */
    @Transactional(readOnly = true)
    public Optional<ConsentView> currentFor(String visitorId) {
        if (visitorId == null || visitorId.isBlank()) {
            return Optional.empty();
        }
        return consentRecordRepository
                .findFirstByVisitorIdAndTenantIdOrderByIdDesc(visitorId.trim(), TenantContext.currentTenantId())
                .map(ConsentView::from);
    }

    /** Full decision history for a subject, for inclusion in their data export. */
    @Transactional(readOnly = true)
    public List<ConsentView> historyForEmail(String email, Long tenantId) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return consentRecordRepository.findByEmailIgnoreCaseAndTenantIdOrderByIdDesc(email, tenantId)
                .stream()
                .map(ConsentView::from)
                .toList();
    }

    /** Consent rows are erased outright — there is no retention basis for keeping them. */
    @Transactional
    public int deleteForEmail(String email, Long tenantId) {
        List<ConsentRecord> records = consentRecordRepository.findByEmailIgnoreCaseAndTenantIdOrderByIdDesc(email, tenantId);
        consentRecordRepository.deleteAll(records);
        return records.size();
    }

    private static String resolveSource(String source) {
        return ConsentRecord.SOURCE_ACCOUNT.equalsIgnoreCase(source)
                ? ConsentRecord.SOURCE_ACCOUNT
                : ConsentRecord.SOURCE_BANNER;
    }

    private static String normalizeOrNull(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
