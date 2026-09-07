package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Not exposed over REST — managed only through NewsletterController. */
@RepositoryRestResource(exported = false)
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    // Scoped to tenant (roadmap #21, Milestone D): email isn't unique across tenants, so an unscoped
    // lookup could merge/leak a same-email subscriber across two different storefronts.
    NewsletterSubscriber findByEmailAndTenantId(String email, Long tenantId);

    // Unscoped by design: the token is a random UUID, globally unique regardless of tenant.
    NewsletterSubscriber findByUnsubscribeToken(String unsubscribeToken);

    // Unscoped by design: only read by the weekly-blast scheduler, a background job with no per-tenant
    // request context that emails every subscriber across every tenant (see WeeklyAdScheduler).
    List<NewsletterSubscriber> findBySubscribedTrue();

    long countByTenantIdAndSubscribedTrue(Long tenantId);
}
