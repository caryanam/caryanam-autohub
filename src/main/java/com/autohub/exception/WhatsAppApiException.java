package com.autohub.exception;


/**
 * Thrown when Meta's API returns a non-2xx response after all retries are exhausted,
 * or the call fails for a non-retryable reason. Caught at the listener boundary -
 * never propagates to the HTTP request thread since this runs async, post-commit.
 */
public class WhatsAppApiException extends RuntimeException {

    private final String responseBody;

    public WhatsAppApiException(String message, String responseBody, Throwable cause) {
        super(message, cause);
        this.responseBody = responseBody;
    }

    public WhatsAppApiException(String message, String responseBody) {
        super(message);
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }
}