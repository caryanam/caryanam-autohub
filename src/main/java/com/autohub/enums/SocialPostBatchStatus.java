package com.autohub.enums;

/**
 * Overall status of a batch created when an admin clicks
 * "Approve &amp; Publish" for a set of vehicle post requests.
 */
public enum SocialPostBatchStatus {
    QUEUED,
    PROCESSING,
    PARTIALLY_COMPLETED,
    COMPLETED,
    FAILED
}
