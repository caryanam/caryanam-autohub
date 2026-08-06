package com.autohub.enums;

/**
 * Determines which WhatsApp template to use for dealer offer broadcasts.
 * IMAGE = caryanam_dealer_offers (header: image)
 * VIDEO = caryanam_dealer_offers_video (header: video)
 */
public enum OfferTemplateType {
    IMAGE,  // caryanam_dealer_offers (existing)
    VIDEO   // caryanam_dealer_offers_video (new)
}
