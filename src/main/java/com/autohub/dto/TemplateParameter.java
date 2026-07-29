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
        return new TemplateParameter("text", cleanMetaParam(value));
    }

    private static String cleanMetaParam(String value) {
        if (value == null) return "";
        // 1. Replace newlines, tabs, and carriage returns with a single space
        String cleaned = value.replaceAll("[\r\n\t]", " ");
        // 2. Replace multiple consecutive spaces (2 or more) with a single space
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        // 3. Trim leading/trailing spaces
        return cleaned.trim();
    }
}
