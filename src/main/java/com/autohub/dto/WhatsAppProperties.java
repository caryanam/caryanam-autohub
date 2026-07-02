package com.autohub.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed binding for whatsapp.* properties in application.properties.
 * @Validated ensures the app fails fast at startup if mandatory config is missing.
 */
@ConfigurationProperties(prefix = "whatsapp")
@Validated
public record WhatsAppProperties(

        @NotBlank(message = "whatsapp.access-token must be configured")
        String accessToken,

        @NotBlank(message = "whatsapp.phone-number-id must be configured")
        String phoneNumberId,

        @NotBlank(message = "whatsapp.api-version must be configured")
        String apiVersion,

        @NotBlank(message = "whatsapp.template-name must be configured")
        String templateName,

        @NotBlank(message = "whatsapp.language-code must be configured")
        String languageCode,

        @NotBlank(message = "whatsapp.base-url must be configured")
        String baseUrl,

        int connectTimeoutMs,

        int readTimeoutMs
) {
    public String messagesEndpoint() {
        return "%s/%s/%s/messages".formatted(baseUrl, apiVersion, phoneNumberId);
    }
}