package com.bob.ecommerceangularapp.dao;

import com.bob.ecommerceangularapp.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Order history. The collection path /api/orders/** is protected by SecurityConfig
 * once an Okta issuer URI is configured (Milestone 3). Its item resource (GET/PUT/DELETE
 * /api/orders/{id}) is tenant-guarded by {@code TenantResourceGuardFilter} (roadmap #21); every
 * derived-query search method below is unexported — see the comment above the *AndTenantId group —
 * so the only ways to read an order are that guarded item resource or a custom, tenant/email-scoped
 * controller endpoint ({@code AccountController}, {@code OrderHistoryController}).
 */
@RepositoryRestResource(collectionResourceRel = "orders", path = "orders")
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Backs OrderHistoryController — customer order lookup, tenant + email scoped. Unexported: SDR
    // would otherwise take BOTH email and tenantId as caller-supplied query params, letting anyone
    // read any tenant's customer's full order history (see the class javadoc's tenant note and
    // AccountController's javadoc for why "email" alone is this app's accepted identity proof —
    // tenantId must never be one of those caller-suppliable values).
    @RestResource(exported = false)
    Page<Order> findByCustomerEmailAndTenantIdOrderByDateCreatedDesc(
            @Param("email") String email, @Param("tenantId") Long tenantId, Pageable pageable);

    // Used by the returns flow; not exposed as a public Spring Data REST search.
    @RestResource(exported = false)
    Optional<Order> findByOrderTrackingNumber(String orderTrackingNumber);

    // Called only from AdminService/AnalyticsService with TenantContext.currentTenantId() — never a
    // caller-controlled value. The query itself is tenant-scoped (`where o.tenantId = :tenantId`), but
    // that scoping is worthless as a public SDR search resource: @Query methods are exported by SDR
    // like any derived-query method, binding :tenantId straight from the request's ?tenantId= query
    // param, so an unauthenticated caller could pass ANY tenant's id and read its total revenue
    // directly (confirmed live via a real-filter-chain probe, 2026-09-26 — GET
    // /api/orders/search/sumTotalRevenue?tenantId=<any id>, no tenant header, no auth, returned 200
    // with that tenant's real revenue figure). @RestResource(exported = false) closes it with zero
    // change to the two Java call sites.
    @RestResource(exported = false)
    @Query("select coalesce(sum(o.totalPrice), 0) from Order o where o.tenantId = :tenantId")
    BigDecimal sumTotalRevenue(@Param("tenantId") Long tenantId);

    /**
     * Backs {@code TenantResourceGuardFilter} — closes the SDR {@code findById} gap (roadmap #21).
     * Unexported, like every {@code *TenantId} method below: each is called only from Java service
     * code with {@code TenantContext.currentTenantId()} — a caller-supplied {@code tenantId} search
     * param would otherwise let anyone read any tenant's order collection/revenue directly, bypassing
     * {@code TenantContext}/{@code TenantResolutionFilter} entirely.
     */
    @RestResource(exported = false)
    boolean existsByIdAndTenantId(Long id, Long tenantId);

    // ----- admin back office + analytics, tenant-scoped (roadmap #21, Milestone B) -----
    @RestResource(exported = false)
    Page<Order> findAllByTenantIdOrderByDateCreatedDesc(Long tenantId, Pageable pageable);

    @RestResource(exported = false)
    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);

    @RestResource(exported = false)
    long countByTenantId(Long tenantId);

    @RestResource(exported = false)
    List<Order> findByTenantId(Long tenantId);

    @RestResource(exported = false)
    List<Order> findByTenantIdAndDateCreatedGreaterThanEqual(Long tenantId, Date cutoff);
}
