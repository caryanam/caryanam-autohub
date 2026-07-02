package com.autohub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents one structural piece of the template (header/body/footer/button).
 * Our approved template only uses a BODY component with 3 text parameters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateComponent(

        @JsonProperty("type")
        String type,

        @JsonProperty("parameters")
        List<TemplateParameter> parameters
) {
    public static TemplateComponent body(List<TemplateParameter> parameters) {
        return new TemplateComponent("body", parameters);
    }
}