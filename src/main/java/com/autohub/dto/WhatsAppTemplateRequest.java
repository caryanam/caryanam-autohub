package com.autohub.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Root payload sent to POST /{api-version}/{phone-number-id}/messages
 */
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

    /**
     * Factory method - builds the exact structure Meta expects for our
     * 'new_lead_generated' approved template (3 body parameters).
     */
    public static WhatsAppTemplateRequest forNewLead(
            String toMobileE164,
            String templateName,
            String languageCode,
            String customerName,
            String customerMobile,
            String vehicleName) {

        List<TemplateParameter> params = List.of(
                TemplateParameter.ofText(customerName),
                TemplateParameter.ofText(customerMobile),
                TemplateParameter.ofText(vehicleName)
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
}
