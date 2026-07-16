package com.autohub.enums;

/**
 * Facebook publishing lifecycle of a single vehicle post request.
 * Distinct from {@link SocialPostApprovalStatus} - a request can be
 * APPROVED (approval) while still NOT_STARTED / QUEUED (publishing).
 */
public enum SocialPostPublishStatus {
    NOT_STARTED,
    QUEUED,
    PROCESSING,
    PUBLISHED,
    FAILED,
    RETRY_SCHEDULED
}
