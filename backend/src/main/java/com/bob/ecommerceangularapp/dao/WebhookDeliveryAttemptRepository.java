package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.WebhookDeliveryAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Append-only delivery-attempt ledger (roadmap #23, Milestone D) — served via AdminWebhookController, not SDR. */
@RepositoryRestResource(exported = false)
public interface WebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttempt, Long> {

    Page<WebhookDeliveryAttempt> findAllByWebhookSubscriptionIdAndTenantIdOrderByAttemptedAtDesc(
            Long webhookSubscriptionId, Long tenantId, Pageable pageable);
}
