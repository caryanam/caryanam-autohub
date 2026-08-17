package com.autohub.configuration.ratelimit;

import com.autohub.enums.RateLimitType;

import java.lang.annotation.*;

/**
 * Declarative rate-limiting annotation.
 * <p>
 * Apply to controller methods (or at class level) to enforce
 * token-bucket rate limits via {@link RateLimitInterceptor}.
 * </p>
 *
 * <pre>
 * &#64;RateLimit(capacity = 5, refillTokens = 5, refillDurationInSeconds = 600, type = RateLimitType.IP_AND_ENDPOINT)
 * &#64;PostMapping("/send-otp")
 * public ResponseEntity&lt;String&gt; sendOtp(...)
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum number of tokens the bucket can hold.
     */
    long capacity() default 60;

    /**
     * Number of tokens added on each refill.
     */
    long refillTokens() default 60;

    /**
     * Duration (in seconds) of the refill interval.
     */
    long refillDurationInSeconds() default 60;

    /**
     * Strategy used to resolve the rate-limiting key.
     */
    RateLimitType type() default RateLimitType.IP;
}
