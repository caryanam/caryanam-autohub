package com.autohub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from properties prefixed "instagram.*" (see application.properties).
 * Values are expected to come from environment variables in every real
 * environment - never hardcode accessToken.
 */
@ConfigurationProperties(prefix = "instagram")
@Validated
public record InstagramProperties(

        @NotBlank(message = "instagram.account-id must be configured")
        String accountId,

        @NotBlank(message = "instagram.access-token must be configured")
        String accessToken,

        @NotBlank(message = "instagram.graph-version must be configured")
        String graphVersion,

        @Min(value = 1, message = "instagram.batch-limit must be at least 1")
        int batchLimit,

        String baseUrl,

        int maxRetryCount,

        String websiteBaseUrl,

        int connectTimeoutMs,

        int readTimeoutMs
) {
    public String mediaCreateEndpoint() {
        return "%s/%s/%s/media".formatted(baseUrl, graphVersion, accountId);
    }

    public String mediaPublishEndpoint() {
        return "%s/%s/%s/media_publish".formatted(baseUrl, graphVersion, accountId);
    }
}
