package com.webknot.kpi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Fallback cache configuration when Redis is disabled.
 * Uses in-memory cache instead.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "false")
public class FallbackCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Use simple in-memory cache when Redis is not available
        return new ConcurrentMapCacheManager(
            "kpi-definitions",
            "webknot-values",
            "certifications",
            "band-directory",
            "stream-directory",
            "employees",
            "employee-by-id",
            "designation-lookup",
            "designation-lookups-by-stream",
            "designation-lookups-by-band",
            "designation-lookups"
        );
    }
}
