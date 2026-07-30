package com.autohub.configuration;

import com.autohub.dto.WhatsAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class WhatsAppBirthdayClient {

    private final WebClient whatsAppWebClient;
    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;

    public WhatsAppBirthdayClient(WebClient whatsAppWebClient,
                                 WhatsAppProperties properties,
                                 ObjectMapper objectMapper) {
        this.whatsAppWebClient = whatsAppWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public BirthdaySendResult sendBirthdayWish(
            String toMobileE164,
            String dealerName // {{1}}
    ) {
        try {
            String payload = """
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "template",
                      "template": {
                        "name": "%s",
                        "language": { "code": "%s" },
                        "components": [
                          {
                            "type": "body",
                            "parameters": [
                              { "type": "text", "text": "%s" }
                            ]
                          }
                        ]
                      }
                    }
                    """.formatted(
                    toMobileE164,
                    properties.birthdayTemplateName(),
                    properties.birthdayLanguageCode(),
                    escapeJson(truncate(dealerName, 100))
            );

            String response = whatsAppWebClient.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            properties.apiVersion(), properties.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            String messageId = node.path("messages").get(0).path("id").asText();

            log.info("Birthday wish sent to [{}]. messageId=[{}]", toMobileE164, messageId);
            return new BirthdaySendResult(true, messageId, response, null);

        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Failed to send birthday wish to [{}]: status={} body={}",
                    toMobileE164, ex.getStatusCode(), body);
            return new BirthdaySendResult(false, null, body, ex.getMessage());

        } catch (Exception ex) {
            log.error("Unexpected error sending birthday wish to [{}]: {}", toMobileE164, ex.getMessage(), ex);
            return new BirthdaySendResult(false, null, null, ex.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\s\\p{Zs}]+", " ");
        cleaned = cleaned.replace("\\r", " ").replace("\\n", " ").replace("\\t", " ");
        cleaned = cleaned.replaceAll(" +", " ").trim();
        return cleaned.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public record BirthdaySendResult(
            boolean success,
            String whatsappMessageId,
            String responsePayload,
            String errorMessage
    ) {}
}
