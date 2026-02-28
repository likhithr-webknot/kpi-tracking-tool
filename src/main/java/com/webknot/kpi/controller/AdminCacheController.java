package com.webknot.kpi.controller;

import com.webknot.kpi.service.CacheInvalidationService;
import com.webknot.kpi.util.RateLimitService;
import com.webknot.kpi.util.RedisService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/cache")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class AdminCacheController {

    private final CacheInvalidationService cacheInvalidationService;
    private final RedisService redisService;
    private final RateLimitService rateLimitService;

    public AdminCacheController(
            CacheInvalidationService cacheInvalidationService,
            RedisService redisService,
            RateLimitService rateLimitService) {
        this.cacheInvalidationService = cacheInvalidationService;
        this.redisService = redisService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("redisAvailable", redisService.isAvailable());
        stats.put("message", "Cache statistics retrieved successfully");
        stats.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/invalidate-all")
    public ResponseEntity<?> invalidateAll() {
        try {
            cacheInvalidationService.invalidateAll();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "All caches have been invalidated",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to invalidate caches: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/invalidate/kpi")
    public ResponseEntity<?> invalidateKpiDefinitions() {
        try {
            cacheInvalidationService.invalidateKpiDefinitions();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "KPI Definitions cache invalidated"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/invalidate/certifications")
    public ResponseEntity<?> invalidateCertifications() {
        try {
            cacheInvalidationService.invalidateCertifications();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Certifications cache invalidated"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/invalidate/band")
    public ResponseEntity<?> invalidateBandDirectory() {
        try {
            cacheInvalidationService.invalidateBandDirectory();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Band Directory cache invalidated"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/invalidate/stream")
    public ResponseEntity<?> invalidateStreamDirectory() {
        try {
            cacheInvalidationService.invalidateStreamDirectory();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Stream Directory cache invalidated"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/invalidate/webknot")
    public ResponseEntity<?> invalidateWebknotValues() {
        try {
            cacheInvalidationService.invalidateWebknotValues();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Webknot Values cache invalidated"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/invalidate/employees")
    public ResponseEntity<?> invalidateEmployees() {
        try {
            cacheInvalidationService.invalidateEmployees();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Employee caches invalidated"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/redis/status")
    public ResponseEntity<?> getRedisStatus() {
        boolean isAvailable = redisService.isAvailable();
        return ResponseEntity.ok(Map.of(
                "status", isAvailable ? "connected" : "disconnected",
                "available", isAvailable,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/rate-limit/reset/{key}")
    public ResponseEntity<?> resetRateLimit(@PathVariable String key) {
        try {
            rateLimitService.reset(key);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Rate limit reset for: " + key
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/rate-limit/status/{key}")
    public ResponseEntity<?> getRateLimitStatus(
            @PathVariable String key,
            @RequestParam(defaultValue = "5") int maxAttempts) {
        try {
            long remaining = rateLimitService.getRemainingAttempts(key, maxAttempts);
            return ResponseEntity.ok(Map.of(
                    "key", key,
                    "maxAttempts", maxAttempts,
                    "remainingAttempts", remaining,
                    "allowed", remaining > 0
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
