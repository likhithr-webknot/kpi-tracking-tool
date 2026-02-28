package com.webknot.kpi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final boolean redisAvailable;

    public RedisService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.redisAvailable = checkRedisAvailability();
    }

    private boolean checkRedisAvailability() {
        if (redisTemplate.isEmpty()) {
            return false;
        }
        try {
            var connection = redisTemplate.get().getConnectionFactory().getConnection();
            if (connection != null) {
                try {
                    connection.ping();
                    log.info("Redis is available");
                    return true;
                } finally {
                    connection.close();
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }

    public void setWithTTL(String key, Object value, long timeout, TimeUnit unit) {
        if (!redisAvailable) {
            log.debug("Redis unavailable - skipping SET operation for key: {}", key);
            return;
        }
        try {
            redisTemplate.ifPresent(rt -> rt.opsForValue().set(key, value, timeout, unit));
            log.debug("Redis SET: key={}, ttl={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to set Redis key {}: {}", key, e.getMessage());
        }
    }

    public Object get(String key) {
        if (!redisAvailable) {
            log.debug("Redis unavailable - skipping GET operation for key: {}", key);
            return null;
        }
        try {
            return redisTemplate.map(rt -> rt.opsForValue().get(key)).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get Redis key {}: {}", key, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public void delete(String key) {
        if (!redisAvailable) {
            log.debug("Redis unavailable - skipping DELETE operation for key: {}", key);
            return;
        }
        try {
            redisTemplate.ifPresent(rt -> rt.delete(key));
            log.debug("Redis DELETE: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to delete Redis key {}: {}", key, e.getMessage());
        }
    }

    public void deleteMultiple(String... keys) {
        if (!redisAvailable || keys == null || keys.length == 0) {
            return;
        }
        try {
            redisTemplate.ifPresent(rt -> {
                for (String key : keys) {
                    rt.delete(key);
                }
            });
            log.debug("Redis DELETE: deleted {} keys", keys.length);
        } catch (Exception e) {
            log.warn("Failed to delete multiple Redis keys: {}", e.getMessage());
        }
    }

    public long increment(String key) {
        if (!redisAvailable) {
            log.debug("Redis unavailable - skipping INCREMENT operation for key: {}", key);
            return 0;
        }
        try {
            Long value = redisTemplate.map(rt -> rt.opsForValue().increment(key)).orElse(null);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.warn("Failed to increment Redis key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    public long incrementWithTTL(String key, long timeout, TimeUnit unit) {
        long value = increment(key);
        if (value == 1 && redisAvailable) {
            try {
                redisTemplate.ifPresent(rt -> rt.expire(key, timeout, unit));
            } catch (Exception e) {
                log.warn("Failed to set TTL for Redis key {}: {}", key, e.getMessage());
            }
        }
        return value;
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        if (!redisAvailable) {
            log.debug("Redis unavailable - skipping EXPIRE operation for key: {}", key);
            return;
        }
        try {
            redisTemplate.ifPresent(rt -> rt.expire(key, timeout, unit));
            log.debug("Redis EXPIRE: key={}, ttl={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to set expiration for Redis key {}: {}", key, e.getMessage());
        }
    }

    public boolean exists(String key) {
        if (!redisAvailable) {
            return false;
        }
        try {
            Boolean exists = redisTemplate.map(rt -> rt.hasKey(key)).orElse(null);
            return exists != null && exists;
        } catch (Exception e) {
            log.warn("Failed to check Redis key existence {}: {}", key, e.getMessage());
            return false;
        }
    }

    public void setHashField(String key, String field, Object value) {
        if (!redisAvailable) {
            return;
        }
        try {
            redisTemplate.ifPresent(rt -> rt.opsForHash().put(key, field, value));
            log.debug("Redis HSET: key={}, field={}", key, field);
        } catch (Exception e) {
            log.warn("Failed to set hash field in Redis key {}: {}", key, e.getMessage());
        }
    }

    public Object getHashField(String key, String field) {
        if (!redisAvailable) {
            return null;
        }
        try {
            return redisTemplate.map(rt -> rt.opsForHash().get(key, field)).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get hash field from Redis key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void deleteHashField(String key, String field) {
        if (!redisAvailable) {
            return;
        }
        try {
            redisTemplate.ifPresent(rt -> rt.opsForHash().delete(key, field));
            log.debug("Redis HDEL: key={}, field={}", key, field);
        } catch (Exception e) {
            log.warn("Failed to delete hash field from Redis key {}: {}", key, e.getMessage());
        }
    }

    public boolean isAvailable() {
        return redisAvailable;
    }
}
