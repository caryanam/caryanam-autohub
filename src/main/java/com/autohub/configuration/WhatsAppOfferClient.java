package com.autohub.configuration;

import com.autohub.dto.WhatsAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Handles two Meta API calls for offer broadcasting:
 * 1. Upload image to Meta's media endpoint → get back a media handle ID
 * 2. Send template message using that handle in the header component
 *
 * Image upload happens ONCE per broadcast — the returned handle is reused
 * for every dealer in that broadcast, avoiding redundant uploads.
 */
@Component
@Slf4j
public class WhatsAppOfferClient {

    private final WebClient whatsAppWebClient;
    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;

    public WhatsAppOfferClient(WebClient whatsAppWebClient,
                               WhatsAppProperties properties,
                               ObjectMapper objectMapper) {
        this.whatsAppWebClient = whatsAppWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Step 1: Upload image bytes to Meta's media endpoint.
     * Returns the media handle (ID) to be used in template header.
     * Called once per offer broadcast — NOT once per dealer.
     */
    public String uploadImageToMeta(byte[] imageBytes, String originalFilename) {
        try {
            String mimeType = resolveMimeType(originalFilename);

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            }).contentType(MediaType.parseMediaType(mimeType));
            bodyBuilder.part("messaging_product", "whatsapp");
            bodyBuilder.part("type", mimeType);

            String response = whatsAppWebClient.post()
                    .uri("/{version}/{phoneNumberId}/media",
                            properties.apiVersion(), properties.phoneNumberId())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            String mediaId = node.get("id").asText();

            log.info("Image uploaded to Meta successfully. mediaId=[{}]", mediaId);
            return mediaId;

        } catch (Exception ex) {
            log.error("Failed to upload offer image to Meta: {}", ex.getMessage(), ex);
            throw new RuntimeException("Meta image upload failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Step 2: Send template message to a single dealer.
     * Uses the mediaId from Step 1 in the HEADER component (image type).
     * Body component carries the 4 text parameters {{1}} to {{4}}.
     */
    public OfferSendResult sendOfferTemplate(
            String toMobileE164,
            String templateName,
            String languageCode,
            String mediaId,
            String dealerName,     // {{1}}
            String offerDetails,   // {{2}}
            String benefits,       // {{3}}
            String contactInfo     // {{4}}
    ) {
        try {
            // Build the exact JSON Meta expects for a template with
            // an IMAGE header + 4 BODY parameters
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
                            "type": "header",
                            "parameters": [
                              {
                                "type": "image",
                                "image": { "id": "%s" }
                              }
                            ]
                          },
                          {
                            "type": "body",
                            "parameters": [
                              { "type": "text", "text": "%s" },
                              { "type": "text", "text": "%s" },
                              { "type": "text", "text": "%s" },
                              { "type": "text", "text": "%s" }
                            ]
                          }
                        ]
                      }
                    }
                    """.formatted(
                    toMobileE164,
                    templateName,
                    languageCode,
                    mediaId,
                    escapeJson(dealerName),
                    escapeJson(offerDetails),
                    escapeJson(benefits),
                    escapeJson(contactInfo)
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

            log.info("Offer template sent to [{}]. messageId=[{}]", toMobileE164, messageId);
            return new OfferSendResult(true, messageId, response, null);

        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Failed to send offer to [{}]: status={} body={}",
                    toMobileE164, ex.getStatusCode(), body);
            return new OfferSendResult(false, null, body, ex.getMessage());

        } catch (Exception ex) {
            log.error("Unexpected error sending offer to [{}]: {}", toMobileE164, ex.getMessage(), ex);
            return new OfferSendResult(false, null, null, ex.getMessage());
        }
    }

    private String resolveMimeType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // default for .jpg/.jpeg
    }

    // Escapes special characters that would break JSON string values
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public record OfferSendResult(
            boolean success,
            String whatsappMessageId,
            String responsePayload,
            String errorMessage
    ) {}
}