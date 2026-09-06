package com.bob.ecommerceangularapp.config;

/**
 * Holds the current request's resolved tenant id (roadmap #21, Milestone A). Set by
 * {@link TenantResolutionFilter} at the start of every request and cleared in a {@code finally} block,
 * the same idiom {@link RequestIdFilter} uses for its MDC correlation id. Any service can read
 * {@link #currentTenantId()} directly — tenant is ambient per-request scoping, not a business
 * parameter callers should have to thread through every method signature.
 *
 * <p>{@link #set}/{@link #clear} are public so a test that calls a service/controller directly
 * (bypassing the real servlet filter chain) can bind a tenant itself, simulating what
 * {@link TenantResolutionFilter} would have done for a real request.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** The current request's tenant id, or {@code null} if none is bound (e.g. outside a request). */
    public static Long currentTenantId() {
        return CURRENT.get();
    }
}
