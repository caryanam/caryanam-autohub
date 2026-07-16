package com.autohub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from properties prefixed "facebook.*" (see application.properties).
 * Values are expected to come from environment variables in every real
 * environment - never hardcode appSecret / pageAccessToken.
 */
@ConfigurationProperties(prefix = "facebook")
@Validated
public record FacebookProperties(

        @NotBlank(message = "facebook.app-id must be configured")
        String appId,

        @NotBlank(message = "facebook.app-secret must be configured")
        String appSecret,

        @NotBlank(message = "facebook.page-id must be configured")
        String pageId,

        @NotBlank(message = "facebook.page-access-token must be configured")
        String pageAccessToken,

        @NotBlank(message = "facebook.graph-version must be configured")
        String graphVersion,

        @Min(value = 1, message = "facebook.batch-limit must be at least 1")
        @Max(value = 10, message = "facebook.batch-limit cannot exceed 10")
        int batchLimit,

        String baseUrl,

        int connectTimeoutMs,

        int readTimeoutMs
) {
    public String photoPublishEndpoint() {
        return "%s/%s/%s/photos".formatted(baseUrl, graphVersion, pageId);
    }
}
