package com.webknot.kpi.util;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {
    private final RedisService redisService;
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";
    private static final String ATTEMPT_COUNT_SUFFIX = ":count";
    private static final String ATTEMPT_TIMESTAMP_SUFFIX = ":timestamp";

    public RateLimitService(RedisService redisService) {
        this.redisService = redisService;
    }

    public boolean isAllowed(String key, int maxAttempts, long windowSeconds) {
        if (!redisService.isAvailable()) {
            return true;
        }

        String counterKey = RATE_LIMIT_KEY_PREFIX + key + ATTEMPT_COUNT_SUFFIX;
        String timestampKey = RATE_LIMIT_KEY_PREFIX + key + ATTEMPT_TIMESTAMP_SUFFIX;

        Long firstAttemptTime = (Long) redisService.get(timestampKey, Long.class);
        long currentTime = System.currentTimeMillis();
        long windowMillis = TimeUnit.SECONDS.toMillis(windowSeconds);

        if (firstAttemptTime == null || (currentTime - firstAttemptTime) > windowMillis) {
            redisService.setWithTTL(timestampKey, currentTime, windowSeconds, TimeUnit.SECONDS);
            redisService.setWithTTL(counterKey, 1, windowSeconds, TimeUnit.SECONDS);
            return true;
        }

        long attemptCount = redisService.incrementWithTTL(counterKey, windowSeconds, TimeUnit.SECONDS);
        return attemptCount < maxAttempts;
    }

    public long getRemainingAttempts(String key, int maxAttempts) {
        if (!redisService.isAvailable()) {
            return maxAttempts;
        }

        String counterKey = RATE_LIMIT_KEY_PREFIX + key + ATTEMPT_COUNT_SUFFIX;
        Long currentCount = (Long) redisService.get(counterKey, Long.class);
        if (currentCount == null) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - currentCount);
    }

    public void reset(String key) {
        String counterKey = RATE_LIMIT_KEY_PREFIX + key + ATTEMPT_COUNT_SUFFIX;
        String timestampKey = RATE_LIMIT_KEY_PREFIX + key + ATTEMPT_TIMESTAMP_SUFFIX;
        redisService.deleteMultiple(counterKey, timestampKey);
    }

    public void clearAll() {
    }
}
