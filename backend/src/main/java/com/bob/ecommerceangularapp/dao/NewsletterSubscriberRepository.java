package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/** Not exposed over REST — managed only through NewsletterController. */
@RepositoryRestResource(exported = false)
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    NewsletterSubscriber findByEmail(String email);

    NewsletterSubscriber findByUnsubscribeToken(String unsubscribeToken);

    List<NewsletterSubscriber> findBySubscribedTrue();

    long countBySubscribedTrue();
}
