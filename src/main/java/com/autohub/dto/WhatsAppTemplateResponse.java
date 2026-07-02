package com.autohub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps Meta's success response, e.g.:
 * {
 *   "messaging_product": "whatsapp",
 *   "contacts": [{ "input": "...", "wa_id": "..." }],
 *   "messages": [{ "id": "wamid.XXXX" }]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppTemplateResponse(

        @JsonProperty("messaging_product")
        String messagingProduct,

        @JsonProperty("contacts")
        List<Contact> contacts,

        @JsonProperty("messages")
        List<MessageId> messages
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            @JsonProperty("input") String input,
            @JsonProperty("wa_id") String waId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageId(
            @JsonProperty("id") String id
    ) {}

    /** Convenience accessor - the wamid we persist for tracking/delivery status webhooks. */
    public String firstMessageId() {
        return (messages != null && !messages.isEmpty()) ? messages.get(0).id() : null;
    }
}
