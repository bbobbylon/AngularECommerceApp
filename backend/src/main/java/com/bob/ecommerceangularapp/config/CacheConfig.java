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

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CATALOG_SEARCH);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(500));
        return manager;
    }
}
