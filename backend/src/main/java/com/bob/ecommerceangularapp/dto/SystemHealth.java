package com.bob.ecommerceangularapp.dto;

import java.util.List;

/**
 * Friendly system-health summary for the admin back-office. Unlike raw {@code /actuator/health}
 * (which stays terse for unauthenticated callers), this reports the status of each integration the
 * app degrades gracefully around, plus version/uptime.
 *
 * @param status       overall status — UP when the database is reachable, otherwise DOWN
 * @param version      build version (from build-info), or "dev" when not packaged
 * @param profile      active Spring profile(s)
 * @param uptimeSeconds JVM uptime in seconds
 * @param components   per-integration readiness (database, email, payments, auth)
 */
public record SystemHealth(
        String status,
        String version,
        String profile,
        long uptimeSeconds,
        List<ComponentStatus> components) {

    /** One integration's readiness. {@code ready=false} is expected for unconfigured optional features. */
    public record ComponentStatus(String name, boolean ready, String detail) {
    }
}
