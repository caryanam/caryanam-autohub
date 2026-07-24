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
 * Handles two Meta API calls for vehicle detail sharing:
 * 1. Upload vehicle image to Meta media endpoint → get media handle
 * 2. Send caryanam_dealer_vehicles_catalog template to dealer's WhatsApp
 *
 * Template structure (from your approved template):
 * HEADER  → image (uploaded vehicle photo)
 * BODY    → 8 text parameters:
 *   {{1}} Vehicle name (Brand + Model + Variant)
 *   {{2}} Price (formatted with ₹)
 *   {{3}} Registration Year
 *   {{4}} Fuel Type
 *   {{5}} KM Driven (formatted)
 *   {{6}} Location (City)
 *   {{7}} Specifications (Ownership, Category, Status, Finance)
 *   {{8}} Detailed Description
 */
@Component
@Slf4j
public class WhatsAppVehicleClient {

    private final WebClient whatsAppWebClient;
    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;

    public WhatsAppVehicleClient(WebClient whatsAppWebClient,
                                 WhatsAppProperties properties,
                                 ObjectMapper objectMapper) {
        this.whatsAppWebClient = whatsAppWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Uploads vehicle image bytes to Meta media API.
     * Returns the media ID used in the template header.
     */
    public String uploadVehicleImage(byte[] imageBytes,
                                     String filename,
                                     String mimeType) {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            }).contentType(MediaType.parseMediaType(mimeType));
            bodyBuilder.part("messaging_product", "whatsapp");
            bodyBuilder.part("type", mimeType);

            String response = whatsAppWebClient.post()
                    .uri("/{version}/{phoneNumberId}/media",
                            properties.apiVersion(),
                            properties.phoneNumberId())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            String mediaId = node.get("id").asText();

            log.info("Vehicle image uploaded to Meta. mediaId=[{}] file=[{}]",
                    mediaId, filename);
            return mediaId;

        } catch (Exception ex) {
            log.error("Failed to upload vehicle image to Meta: {}", ex.getMessage(), ex);
            throw new RuntimeException(
                    "Vehicle image upload to WhatsApp failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Sends the vehicle catalog template to the dealer's own WhatsApp number.
     */
    public VehicleShareResult sendVehicleCatalogTemplate(
            String toMobileE164,
            String mediaId,
            String vehicleName,       // {{1}}
            String price,             // {{2}}
            String registrationYear,  // {{3}}
            String fuelType,          // {{4}}
            String kmDriven,          // {{5}}
            String location,          // {{6}}
            String specifications,    // {{7}}
            String description        // {{8}}
    ) {
        try {
            // Build exact JSON payload Meta expects:
            // Header component (image) + Body component (8 text params)
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
                              { "type": "text", "text": "%s" },
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
                    properties.vehicleTemplateName(),
                    properties.vehicleLanguageCode(),
                    mediaId,
                    escapeJson(vehicleName),
                    escapeJson(price),
                    escapeJson(registrationYear),
                    escapeJson(fuelType),
                    escapeJson(kmDriven),
                    escapeJson(location),
                    escapeJson(specifications),
                    escapeJson(description)
            );

            String response = whatsAppWebClient.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            properties.apiVersion(),
                            properties.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            String messageId = node.path("messages").get(0).path("id").asText();

            log.info("Vehicle catalog sent to [{}]. messageId=[{}]",
                    toMobileE164, messageId);
            return new VehicleShareResult(true, messageId, response, null);

        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("Meta API rejected vehicle share to [{}]: status={} body={}",
                    toMobileE164, ex.getStatusCode(), body);
            return new VehicleShareResult(false, null, body, ex.getMessage());

        } catch (Exception ex) {
            log.error("Unexpected error sending vehicle catalog to [{}]: {}",
                    toMobileE164, ex.getMessage(), ex);
            return new VehicleShareResult(false, null, null, ex.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record VehicleShareResult(
            boolean success,
            String whatsappMessageId,
            String responsePayload,
            String errorMessage
    ) {}
}