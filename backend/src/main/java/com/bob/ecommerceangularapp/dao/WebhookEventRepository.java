package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** Webhook outbox rows (roadmap #23, Milestones C + D) — served via AdminWebhookController, not SDR. */
@RepositoryRestResource(exported = false)
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByIdAndTenantId(Long id, Long tenantId);

    List<WebhookEvent> findAllByWebhookSubscriptionIdAndTenantIdOrderByDateCreatedDesc(Long webhookSubscriptionId, Long tenantId);

    // Unscoped by design: read only by the delivery scheduler, a background job with no per-tenant
    // request context that sweeps every due event across every tenant, same idiom as
    // TenantBillingAccountRepository.findAllByStatusInAndCurrentPeriodEndLessThanEqual.
    List<WebhookEvent> findAllByStatusInAndNextAttemptAtLessThanEqual(List<String> statuses, Date cutoff);
}
