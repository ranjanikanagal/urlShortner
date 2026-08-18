package com.assessment.urlshortener.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Wraps every Redis call with a bounded retry + a recover fallback that
 * degrades to "cache miss" / "no-op" instead of propagating. Redis is a
 * cache, not the source of truth (Postgres is), so a transient Redis blip
 * should cost read latency, not correctness — this is the piece that makes
 * that true. @Retryable requires calls to go through the Spring proxy, which
 * is why this is its own bean rather than private methods on the service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheClient {

    private static final String CACHE_PREFIX = "url:";

    private final StringRedisTemplate redisTemplate;

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public String get(String shortCode) {
        return redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
    }

    @Recover
    public String recoverGet(Exception e, String shortCode) {
        log.warn("Redis GET failed after retries for shortCode={}, falling back to DB", shortCode, e);
        return null;
    }

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public void put(String shortCode, String originalUrl, Duration ttl) {
        if (!ttl.isNegative()) {
            redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, originalUrl, ttl);
        }
    }

    @Recover
    public void recoverPut(Exception e, String shortCode, String originalUrl, Duration ttl) {
        log.warn("Redis SET failed after retries for shortCode={}, cache left cold", shortCode, e);
    }

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public void evict(String shortCode) {
        redisTemplate.delete(CACHE_PREFIX + shortCode);
    }

    @Recover
    public void recoverEvict(Exception e, String shortCode) {
        log.warn("Redis DELETE failed after retries for shortCode={}, entry will expire via TTL instead", shortCode, e);
    }
}
