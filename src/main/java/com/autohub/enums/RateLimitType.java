package com.autohub.enums;

/**
 * Defines the strategy used to resolve the rate-limiting key.
 */
public enum RateLimitType {

    /**
     * Rate limit by the resolved client IP address.
     */
    IP,

    /**
     * Rate limit by the authenticated user's ID (Dealer ID / Admin ID / Customer ID).
     */
    USER_ID,

    /**
     * Rate limit by a composite key of client IP + request URI path.
     * Useful for per-endpoint throttling (e.g., OTP, password reset).
     */
    IP_AND_ENDPOINT
}
