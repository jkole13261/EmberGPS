package com.embergps.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-device rate limiter for the GPS ingest endpoint.
 * Uses Bucket4j with an in-memory token-bucket per device API key.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int requestsPerMinute;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(
            @Value("${app.rate-limit.ingest-per-minute:120}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket());

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Rate limit exceeded for key prefix: {}", key);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Rate limit exceeded. Max " + requestsPerMinute + " requests/min.\"}");
        return false;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /** Use first 16 chars of the API key as the bucket key to avoid storing full keys in memory. */
    private String resolveKey(HttpServletRequest request) {
        String key = request.getHeader("X-API-Key");
        if (key != null && key.length() >= 16) {
            return key.substring(0, 16);
        }
        return request.getRemoteAddr();
    }
}
