package com.autohub.configuration;

import com.autohub.dto.WhatsAppProperties;
import com.autohub.dto.WhatsAppTemplateRequest;
import com.autohub.dto.WhatsAppTemplateResponse;
import com.autohub.exception.WhatsAppApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class WhatsAppClient {

    private final WebClient whatsAppWebClient;
    private final WhatsAppProperties properties;
    private final RetryTemplate retryTemplate;
    private final ObjectMapper objectMapper;

    public WhatsAppClient(WebClient whatsAppWebClient,
                          WhatsAppProperties properties,
                          RetryTemplate retryTemplate,
                          ObjectMapper objectMapper) {
        this.whatsAppWebClient = whatsAppWebClient;
        this.properties = properties;
        this.retryTemplate = retryTemplate;
        this.objectMapper = objectMapper;
    }

    public WhatsAppApiCallResult sendTemplateMessage(WhatsAppTemplateRequest request) {

        String requestJson = serializeQuietly(request);

        // Explicitly typed RetryCallback - avoids any lambda-inference ambiguity
        RetryCallback<WhatsAppApiCallResult, RuntimeException> retryCallback =
                (RetryContext context) -> {

                    log.info("Attempt [{}] - sending WhatsApp template message to [{}]",
                            context.getRetryCount() + 1, request.to());

                    try {
                        String responseJson = whatsAppWebClient.post()
                                .uri("/{version}/{phoneNumberId}/messages",
                                        properties.apiVersion(), properties.phoneNumberId())
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();

                        WhatsAppTemplateResponse parsed =
                                objectMapper.readValue(responseJson, WhatsAppTemplateResponse.class);

                        log.info("WhatsApp message sent successfully. messageId=[{}]", parsed.firstMessageId());


                        return new WhatsAppApiCallResult(true, requestJson, responseJson,
                                parsed.firstMessageId(), null);

                    } catch (WebClientResponseException.BadRequest
                             | WebClientResponseException.Unauthorized
                             | WebClientResponseException.Forbidden ex) {
                        String body = ex.getResponseBodyAsString();
                        log.error("Permanent WhatsApp API failure (status={}): {}", ex.getStatusCode(), body);
                        throw new WhatsAppApiException(
                                "WhatsApp API rejected the request: " + ex.getStatusCode(), body, ex);

                    } catch (WebClientResponseException ex) {
                        log.warn("Transient WhatsApp API failure (status={}), will retry if attempts remain",
                                ex.getStatusCode());
                        throw ex;

                    } catch (JsonProcessingException ex) {
                        log.error("Failed to parse WhatsApp API response", ex);
                        throw new WhatsAppApiException("Failed to parse WhatsApp API response", null, ex);
                    }
                };

        // Explicitly typed RecoveryCallback - same reasoning
        RecoveryCallback<WhatsAppApiCallResult> recoveryCallback =
                (RetryContext recoveryContext) -> {

                    Throwable lastException = recoveryContext.getLastThrowable();
                    String body = (lastException instanceof WebClientResponseException wcre)
                            ? wcre.getResponseBodyAsString() : null;

                    log.error("All retry attempts exhausted for WhatsApp message to [{}]",
                            request.to(), lastException);

                    return new WhatsAppApiCallResult(false, requestJson, body, null,
                            lastException != null ? lastException.getMessage() : "Unknown error");
                };

        return retryTemplate.execute(retryCallback, recoveryCallback);
    }

    private String serializeQuietly(WhatsAppTemplateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize WhatsApp request for logging purposes", e);
            return "{}";
        }
    }

    public record WhatsAppApiCallResult(
            boolean success,
            String requestPayload,
            String responsePayload,
            String whatsappMessageId,
            String errorMessage
    ) {}
}