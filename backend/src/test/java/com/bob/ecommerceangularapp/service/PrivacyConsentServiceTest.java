package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.ConsentRecordRepository;
import com.bob.ecommerceangularapp.dto.ConsentRequest;
import com.bob.ecommerceangularapp.dto.ConsentView;
import com.bob.ecommerceangularapp.entity.ConsentRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test (mocked repository) for the cookie/storage consent ledger (roadmap #24).
 */
class PrivacyConsentServiceTest {

    private static final Long TENANT_ID = 7L;

    private final ConsentRecordRepository consentRecordRepository = mock(ConsentRecordRepository.class);
    private final PrivacyConsentService service = new PrivacyConsentService(consentRecordRepository, "2026-09-01");

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void record_forcesNecessaryTrueRegardlessOfWhatTheRequestCarries() {
        when(consentRecordRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentView view = service.record(new ConsentRequest("visitor-1", null, true, false, true, "BANNER"));

        assertThat(view.necessary()).isTrue();
        assertThat(view.functional()).isTrue();
        assertThat(view.analytics()).isFalse();
        assertThat(view.marketing()).isTrue();
        assertThat(view.policyVersion()).isEqualTo("2026-09-01");
    }

    @Test
    void record_stampsTheCurrentTenantAndTrimsTheVisitorId() {
        when(consentRecordRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(new ConsentRequest("  visitor-1  ", "Person@Example.com", false, false, false, "BANNER"));

        verify(consentRecordRepository).save(argThatCaptures(record -> {
            assertThat(record.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(record.getVisitorId()).isEqualTo("visitor-1");
            assertThat(record.getEmail()).isEqualTo("person@example.com");
        }));
    }

    @Test
    void record_blankEmail_isStoredAsNull() {
        when(consentRecordRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(new ConsentRequest("visitor-1", "  ", false, false, false, "BANNER"));

        verify(consentRecordRepository).save(argThatCaptures(record -> assertThat(record.getEmail()).isNull()));
    }

    @Test
    void record_unknownSource_defaultsToBanner() {
        when(consentRecordRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(new ConsentRequest("visitor-1", null, false, false, false, "something-else"));

        verify(consentRecordRepository).save(argThatCaptures(
                record -> assertThat(record.getSource()).isEqualTo(ConsentRecord.SOURCE_BANNER)));
    }

    @Test
    void record_accountSource_isPreservedCaseInsensitively() {
        when(consentRecordRepository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(new ConsentRequest("visitor-1", null, false, false, false, "account"));

        verify(consentRecordRepository).save(argThatCaptures(
                record -> assertThat(record.getSource()).isEqualTo(ConsentRecord.SOURCE_ACCOUNT)));
    }

    @Test
    void currentFor_blankVisitorId_isEmptyWithoutHittingTheRepository() {
        assertThat(service.currentFor(" ")).isEmpty();
    }

    @Test
    void currentFor_returnsTheMostRecentDecisionForTheTenant() {
        ConsentRecord latest = ConsentRecord.builder().id(2L).visitorId("visitor-1").necessary(true)
                .functional(true).analytics(false).marketing(false).policyVersion("2026-09-01")
                .source(ConsentRecord.SOURCE_BANNER).build();
        when(consentRecordRepository.findFirstByVisitorIdAndTenantIdOrderByIdDesc("visitor-1", TENANT_ID))
                .thenReturn(Optional.of(latest));

        assertThat(service.currentFor("visitor-1")).contains(ConsentView.from(latest));
    }

    @Test
    void historyForEmail_blankEmail_returnsEmptyListWithoutHittingTheRepository() {
        assertThat(service.historyForEmail(" ", TENANT_ID)).isEmpty();
    }

    @Test
    void deleteForEmail_erasesEveryDecisionOutright_thereIsNoRetentionBasis() {
        ConsentRecord a = ConsentRecord.builder().id(1L).build();
        ConsentRecord b = ConsentRecord.builder().id(2L).build();
        when(consentRecordRepository.findByEmailIgnoreCaseAndTenantIdOrderByIdDesc("person@example.com", TENANT_ID))
                .thenReturn(List.of(a, b));

        int deleted = service.deleteForEmail("person@example.com", TENANT_ID);

        assertThat(deleted).isEqualTo(2);
        verify(consentRecordRepository).deleteAll(List.of(a, b));
    }

    /** Small helper so a save() call can be asserted on the entity Hibernate/JPA would have received. */
    private static ConsentRecord argThatCaptures(java.util.function.Consumer<ConsentRecord> assertions) {
        return org.mockito.ArgumentMatchers.argThat(record -> {
            assertions.accept(record);
            return true;
        });
    }
}
