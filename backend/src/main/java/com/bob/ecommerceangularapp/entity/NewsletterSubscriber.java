package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

/**
 * A standalone newsletter opt-in — someone who joined via the signup box without (yet)
 * being a checkout customer. The weekly blast targets these plus subscribed customers,
 * deduplicated by email.
 */
@Entity
@Table(name = "newsletter_subscriber")
@Getter
@Setter
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "subscribed")
    private boolean subscribed = true;

    @Column(name = "unsubscribe_token", unique = true)
    private String unsubscribeToken;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;

    public String ensureUnsubscribeToken() {
        if (unsubscribeToken == null || unsubscribeToken.isBlank()) {
            unsubscribeToken = UUID.randomUUID().toString().replace("-", "");
        }
        return unsubscribeToken;
    }
}
