package com.webknot.kpi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class CacheInvalidationService {
    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);
    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @CacheEvict(value = "kpi-definitions", allEntries = true)
    public void invalidateKpiDefinitions() {
        log.info("KPI Definitions cache invalidated");
    }

    @CacheEvict(value = "webknot-values", allEntries = true)
    public void invalidateWebknotValues() {
        log.info("Webknot Values cache invalidated");
    }

    @CacheEvict(value = "certifications", allEntries = true)
    public void invalidateCertifications() {
        log.info("Certifications cache invalidated");
    }

    @CacheEvict(value = "band-directory", allEntries = true)
    public void invalidateBandDirectory() {
        log.info("Band Directory cache invalidated");
    }

    @CacheEvict(value = "stream-directory", allEntries = true)
    public void invalidateStreamDirectory() {
        log.info("Stream Directory cache invalidated");
    }

    @CacheEvict(value = {"employees", "employee-by-id"}, allEntries = true)
    public void invalidateEmployees() {
        log.info("Employee caches invalidated");
    }

    @CacheEvict(value = "designation-lookups", allEntries = true)
    public void invalidateDesignationLookups() {
        log.info("Designation Lookups cache invalidated");
    }

    public void invalidateAll() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            log.info("All caches invalidated");
        }
    }

    public String getCacheStats() {
        StringBuilder stats = new StringBuilder("Cache Statistics:\n");
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                stats.append("- ").append(cacheName).append(": ").append(cache != null ? "exists" : "null").append("\n");
            });
        }
        return stats.toString();
    }
}
