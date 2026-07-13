package com.autohub.dto;

/**
 * Published after DealerOffer is saved to DB and transaction commits.
 * Carries the offerId so the async listener can reload full details
 * from DB in its own transaction context — avoids lazy-loading issues
 * and keeps the event payload lightweight.
 */
public record DealerOfferBroadcastEvent(
        Long offerId,
        String dealerGreetingName,
        String offerDetails,
        String benefits,
        String contactInfo,
        String metaImageHandle,
        int totalDealers
) {}