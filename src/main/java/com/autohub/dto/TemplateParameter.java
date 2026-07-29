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
        // 1. Aggressively replace all whitespaces (including \r, \n, \t, and unicode spaces) with a single space
        String cleaned = value.replaceAll("[\\s\\p{Zs}]+", " ");
        // 2. Also replace literal string versions if they were saved as raw backslash-n strings in the DB
        cleaned = cleaned.replace("\\r", " ").replace("\\n", " ").replace("\\t", " ");
        // 3. Collapse any multiple spaces that might have formed from step 2
        cleaned = cleaned.replaceAll(" +", " ");
        // 4. Trim leading/trailing spaces
        return cleaned.trim();
    }
}
