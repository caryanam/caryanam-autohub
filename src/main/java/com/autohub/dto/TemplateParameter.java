package com.autohub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single {{n}} placeholder value inside a template body.
 * Meta requires "type": "text" for plain text substitutions.
 */
public record TemplateParameter(

        @JsonProperty("type")
        String type,

        @JsonProperty("text")
        String text
) {
    public static TemplateParameter ofText(String value) {
        return new TemplateParameter("text", value);
    }
}
