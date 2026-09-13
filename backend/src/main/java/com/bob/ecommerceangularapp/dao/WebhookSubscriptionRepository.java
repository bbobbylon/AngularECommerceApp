package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

/** Webhook subscriptions are served via the custom admin controller, not Spring Data REST. */
@RepositoryRestResource(exported = false)
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findAllByTenantId(Long tenantId);

    Optional<WebhookSubscription> findByIdAndTenantId(Long id, Long tenantId);

    /** Delivery candidates for {@code WebhookEventPublisher} (roadmap #23, Milestone C). */
    List<WebhookSubscription> findAllByTenantIdAndActiveTrue(Long tenantId);
}
