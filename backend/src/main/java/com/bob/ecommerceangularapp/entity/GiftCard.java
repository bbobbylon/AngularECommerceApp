package com.bob.ecommerceangularapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A prepaid gift card / store credit. Redeemed at checkout as a partial (or full) payment: the applied
 * amount draws down {@link #balance} and the rest of the order is paid by card. Identified by a
 * per-tenant-unique {@link #code}; {@link #initialBalance} is kept for reporting.
 */
@Entity
@Table(name = "gift_card", uniqueConstraints = @UniqueConstraint(name = "uk_gift_card_tenant_code", columnNames = {"tenant_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Roadmap #21 (multi-tenancy, Milestone C). See {@link Product#getTenantId()} for the isolation rationale. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "code")
    private String code;

    @Column(name = "initial_balance")
    private BigDecimal initialBalance;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "active")
    private boolean active;

    @Column(name = "date_created")
    @CreationTimestamp
    private Date dateCreated;
}
