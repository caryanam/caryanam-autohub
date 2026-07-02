package com.autohub.dto;

/**
 * Raised immediately after a CustomerLead is saved (but transaction not yet committed).
 * Carries only primitive/ID data - NOT entity references - to avoid lazy-loading
 * issues once the listener runs asynchronously on a different thread/session.
 */
public record LeadCreatedEvent(
        Long leadId,
        Long vehicleId,
        Long dealerId,
        String customerName,
        String customerMobile,
        String vehicleDisplayName,   // e.g. "Hyundai Creta"
        String dealerWhatsappNumber
) {}