package com.autohub.enums;

/**
 * Approval lifecycle of a dealer's request to publish a vehicle to the
 * Facebook Page. Set by the dealer (PENDING) and the admin
 * (APPROVED / REJECTED), or by the system (CANCELLED) when superseded.
 */
public enum SocialPostApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
