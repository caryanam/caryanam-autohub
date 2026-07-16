package com.autohub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps the JSON body returned by POST /{page-id}/photos, e.g.
 * {"id":"1234567890","post_id":"{page-id}_1234567890"}.
 * post_id is not always present depending on the photo's privacy/target,
 * so id is treated as the primary identifier.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FacebookPhotoPublishResponse(
        String id,
        String post_id
) {
    public String effectivePostId() {
        return (post_id != null && !post_id.isBlank()) ? post_id : id;
    }
}
