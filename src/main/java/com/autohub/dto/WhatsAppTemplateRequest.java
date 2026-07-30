package com.autohub.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WhatsAppTemplateRequest(

        @JsonProperty("messaging_product")
        String messagingProduct,

        @JsonProperty("to")
        String to,

        @JsonProperty("type")
        String type,

        @JsonProperty("template")
        TemplatePayload template
) {

    public record TemplatePayload(
            @JsonProperty("name") String name,
            @JsonProperty("language") Language language,
            @JsonProperty("components") List<TemplateComponent> components
    ) {}

    public record Language(
            @JsonProperty("code") String code
    ) {}

    public static WhatsAppTemplateRequest forNewLead(
            String toMobileE164,
            String templateName,
            String languageCode,
            String customerName,
            String customerMobile,
            String vehicleName) {

        List<TemplateParameter> params = List.of(
                TemplateParameter.ofText(truncate(customerName, 100)),
                TemplateParameter.ofText(truncate(customerMobile, 20)),
                TemplateParameter.ofText(truncate(vehicleName, 150))
        );

        TemplateComponent bodyComponent = TemplateComponent.body(params);

        return new WhatsAppTemplateRequest(
                "whatsapp",
                toMobileE164,
                "template",
                new TemplatePayload(
                        templateName,
                        new Language(languageCode),
                        List.of(bodyComponent)
                )
        );
    }

    /**
     * Helper to safely truncate strings to avoid exceeding Meta's 1024 char limit
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "N/A";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
