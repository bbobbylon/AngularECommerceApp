package com.bob.ecommerceangularapp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * In-process caching via Caffeine. Currently backs the faceted catalog search (the most-hit read
 * path) — entries expire quickly and are bounded, and admin product writes evict them, so the cache
 * never serves meaningfully stale data. Swap the manager for Redis to share a cache across instances.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Catalog search results — short TTL so rating/stock changes self-heal even without an evict. */
    public static final String CATALOG_SEARCH = "catalogSearch";

    /** Slug -&gt; {@code Tenant} lookups (roadmap #21) — resolved on every request, so this must be cached. */
    public static final String TENANT_LOOKUP = "tenantLookup";

    /** Raw-key -&gt; {@code ApiKey} lookups (roadmap #23) — resolved on every headless request. */
    public static final String API_KEY_LOOKUP = "apiKeyLookup";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CATALOG_SEARCH, TENANT_LOOKUP, API_KEY_LOOKUP);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(500));
        return manager;
    }
}
