package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
// email is looked up on checkout, account, and newsletter flows; index it (not unique by design).
@Table(name = "customer", indexes = @Index(name = "idx_customer_email", columnList = "email"))
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    /** Opt-in to the weekly marketing email. Defaults to subscribed for new customers. */
    @Column(name = "newsletter_subscribed")
    private boolean newsletterSubscribed = true;

    /** Opaque token used for one-click unsubscribe links in marketing email. */
    @Column(name = "unsubscribe_token", unique = true)
    private String unsubscribeToken;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Order> orders = new HashSet<>();

    public void add(Order order) {
        if (order != null) {
            orders.add(order);
            order.setCustomer(this);
        }
    }

    /** Lazily assigns an unsubscribe token so existing rows backfill on next save. */
    public String ensureUnsubscribeToken() {
        if (unsubscribeToken == null || unsubscribeToken.isBlank()) {
            unsubscribeToken = UUID.randomUUID().toString().replace("-", "");
        }
        return unsubscribeToken;
    }
}
