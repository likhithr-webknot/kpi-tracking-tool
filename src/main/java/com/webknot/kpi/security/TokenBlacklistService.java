package com.webknot.kpi.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token Blacklist Service
 * 
 * Stores revoked JWT tokens in Redis with automatic expiration.
 * Falls back to in-memory storage if Redis is not available.
 */
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean redisEnabled;

    public TokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = isRedisAvailable();
    }

    public void revokeToken(String token, Instant expiresAt) {
        if (redisEnabled) {
            // Store in Redis with expiration
            long ttlMs = Math.max(0, expiresAt.toEpochMilli() - System.currentTimeMillis());
            long ttlSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(ttlMs));
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, true, ttlSeconds, TimeUnit.SECONDS);
        }
        // Also store in memory as fallback
        revokedTokens.put(token, expiresAt);
    }

    public boolean isRevoked(String token) {
        // Check Redis first
        if (redisEnabled) {
            Boolean revoked = (Boolean) redisTemplate.opsForValue().get(BLACKLIST_PREFIX + token);
            if (revoked != null && revoked) {
                return true;
            }
        }

        // Fallback to in-memory storage
        Instant expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revokedTokens.remove(token);
            return false;
        }
        return true;
    }

    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
