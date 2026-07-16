package com.autohub.exception;

/**
 * Thrown when the Facebook Graph API returns a non-2xx response after all
 * retries are exhausted, or the call fails for a non-retryable reason.
 * Always raised and caught inside the async publish worker - never
 * propagates to the HTTP request thread, since publishing runs post-commit.
 */
public class FacebookApiException extends RuntimeException {

    private final String responseBody;

    /**
     * True for failures that will never succeed on retry (invalid/expired
     * token, missing permission, invalid image URL) - drives the
     * SocialPostBatchItem.retryable flag. False for transient failures
     * (network, timeout, 5xx, rate limit) that are safe to retry.
     */
    private final boolean permanent;

    public FacebookApiException(String message, String responseBody, boolean permanent, Throwable cause) {
        super(message, cause);
        this.responseBody = responseBody;
        this.permanent = permanent;
    }

    public FacebookApiException(String message, String responseBody, boolean permanent) {
        super(message);
        this.responseBody = responseBody;
        this.permanent = permanent;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isPermanent() {
        return permanent;
    }
}
