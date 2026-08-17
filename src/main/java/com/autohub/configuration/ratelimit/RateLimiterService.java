package com.autohub.configuration.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Manages per-key token buckets backed by a Caffeine in-memory cache.
 * <p>
 * Each unique key (e.g., IP address, IP+endpoint combo, or user ID)
 * gets its own Bucket with a configurable token-bucket policy.
 * Stale buckets are automatically evicted after 30 minutes of inactivity.
 * </p>
 */
@Slf4j
@Service
public class RateLimiterService {

    /**
     * In-memory cache: key → Bucket.
     * Entries expire 30 minutes after last access to prevent memory leaks.
     */
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(100_000)
            .build();

    /**
     * Result of a rate-limit consumption check.
     *
     * @param allowed        whether the request is allowed
     * @param remainingTokens tokens remaining after this consumption
     * @param retryAfterNanos nanoseconds until the next token is available (0 if allowed)
     */
    public record ConsumptionResult(boolean allowed, long remainingTokens, long retryAfterNanos) {
    }

    /**
     * Attempts to consume one token from the bucket identified by {@code key}.
     * If no bucket exists for the key, one is created with the given parameters.
     *
     * @param key                   the rate-limit key (e.g., "IP:192.168.1.1" or "IP_EP:10.0.0.1:/api/auth/send-otp")
     * @param capacity              maximum tokens the bucket can hold
     * @param refillTokens          tokens added per refill interval
     * @param refillDurationSeconds duration (seconds) of the refill interval
     * @return a {@link ConsumptionResult} indicating whether the request is allowed
     */
    public ConsumptionResult tryConsume(String key, long capacity, long refillTokens, long refillDurationSeconds) {

        Bucket bucket = bucketCache.get(key, k -> createBucket(capacity, refillTokens, refillDurationSeconds));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new ConsumptionResult(true, probe.getRemainingTokens(), 0);
        }

        long retryAfterNanos = probe.getNanosToWaitForRefill();
        log.warn("Rate limit exceeded for key [{}]. Retry after {} ms", key, retryAfterNanos / 1_000_000);
        return new ConsumptionResult(false, 0, retryAfterNanos);
    }

    /**
     * Creates a token bucket with a greedy refill strategy.
     */
    private Bucket createBucket(long capacity, long refillTokens, long durationSeconds) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofSeconds(durationSeconds))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
