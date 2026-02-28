package com.webknot.kpi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final boolean redisEnabled;

    public TokenBlacklistService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.redisEnabled = isRedisAvailable();
    }

    public void revokeToken(String token, Instant expiresAt) {
        if (redisEnabled) {
            long ttlMs = Math.max(0, expiresAt.toEpochMilli() - System.currentTimeMillis());
            long ttlSeconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(ttlMs));
            redisTemplate.ifPresent(rt -> rt.opsForValue().set(BLACKLIST_PREFIX + token, true, ttlSeconds, TimeUnit.SECONDS));
        }
        revokedTokens.put(token, expiresAt);
    }

    public boolean isRevoked(String token) {
        if (redisEnabled) {
            Boolean revoked = redisTemplate.map(rt -> (Boolean) rt.opsForValue().get(BLACKLIST_PREFIX + token)).orElse(null);
            if (revoked != null && revoked) {
                return true;
            }
        }

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
        if (redisTemplate.isEmpty()) {
            return false;
        }
        try {
            var connection = redisTemplate.get().getConnectionFactory().getConnection();
            if (connection != null) {
                try {
                    connection.ping();
                    return true;
                } finally {
                    connection.close();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
