package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.AuditLogRepository;
import com.bob.ecommerceangularapp.dto.AuditLogView;
import com.bob.ecommerceangularapp.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit test (mocked repository) for the global admin audit log (roadmap #19). */
class AuditLogServiceTest {

    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final AuditLogService service = new AuditLogService(auditLogRepository);

    @Test
    void recordResolvesActorFromAnAuthenticatedPrincipal() {
        Authentication auth = new TestingAuthenticationToken("admin@example.com", "n/a");
        auth.setAuthenticated(true);

        service.record(auth, "PRODUCT_CREATE", "Product", "101", "Test Mug");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getActor()).isEqualTo("admin@example.com");
        assertThat(entry.getAction()).isEqualTo("PRODUCT_CREATE");
        assertThat(entry.getEntityType()).isEqualTo("Product");
        assertThat(entry.getEntityId()).isEqualTo("101");
        assertThat(entry.getDetails()).isEqualTo("Test Mug");
    }

    @Test
    void recordFallsBackToAnonymousWhenThereIsNoAuthentication() {
        service.record(null, "PRODUCT_DELETE", "Product", "101", null);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("anonymous");
    }

    @Test
    void recordFallsBackToAnonymousForAnAnonymousToken() {
        Authentication anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        service.record(anon, "PRODUCT_DELETE", "Product", "101", null);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("anonymous");
    }

    @Test
    void listDelegatesToTheAllEntriesQueryWhenNoEntityTypeFilterIsGiven() {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId(1L);
        entry.setActor("admin@example.com");
        entry.setAction("PRODUCT_CREATE");
        entry.setEntityType("Product");
        Page<AuditLogEntry> page = new PageImpl<>(List.of(entry));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        Page<AuditLogView> result = service.list(PageRequest.of(0, 20), null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).actor()).isEqualTo("admin@example.com");
    }

    @Test
    void listFiltersByEntityTypeWhenGiven() {
        Page<AuditLogEntry> page = new PageImpl<>(List.of());
        when(auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(any(), any())).thenReturn(page);

        service.list(PageRequest.of(0, 20), "Product");

        verify(auditLogRepository).findByEntityTypeOrderByCreatedAtDesc("Product", PageRequest.of(0, 20));
    }
}
