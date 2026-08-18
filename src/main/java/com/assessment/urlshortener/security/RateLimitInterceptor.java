package com.assessment.urlshortener.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

/**
 * Fixed-window rate limiter keyed by client IP, backed by Redis so it works
 * correctly across multiple app instances (an in-memory counter would let
 * each instance grant its own separate quota). Redis INCR is atomic, so no
 * extra locking is needed for the increment itself.
 *
 * Fixed-window (not sliding/token-bucket) was chosen deliberately for this
 * scope: it allows a burst at the window boundary, which a token bucket
 * avoids, but it's one INCR+EXPIRE instead of a Lua script or sorted-set
 * bookkeeping. That trade-off is spelled out in docs/TRADEOFFS.md.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private long requestsPerMinute;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled) {
            return true;
        }

        String clientIp = clientIp(request);
        long windowEpochMinute = Instant.now().getEpochSecond() / 60;
        String key = "ratelimit:%s:%d".formatted(clientIp, windowEpochMinute);

        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
        } catch (Exception e) {
            // Redis being unavailable should not take the whole API down —
            // fail open, same philosophy as the cache-aside read path.
            log.warn("Rate limiter backend unavailable, allowing request through", e);
            return true;
        }

        if (count != null && count > requestsPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            try {
                response.getWriter().write(
                        "{\"error\":\"Rate limit exceeded, try again shortly\"}");
            } catch (Exception ignored) {
                // best-effort body write; status code is already set
            }
            return false;
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
