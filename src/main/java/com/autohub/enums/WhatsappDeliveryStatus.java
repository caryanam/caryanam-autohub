package com.autohub.enums;

/**
 * Tracks the delivery lifecycle of a WhatsApp message
 * as reported back by Meta's webhook callbacks.
 *
 * Lifecycle order: ACCEPTED → SENT → DELIVERED → READ
 * FAILED can occur at any stage.
 */
public enum WhatsappDeliveryStatus {

    /**
     * Meta accepted the API request — message queued internally.
     * This is the default state when we first log a SUCCESS from the API.
     */
    ACCEPTED,

    /**
     * Meta successfully sent the message to the recipient's device.
     * Recipient's phone may be offline — not yet confirmed delivered.
     */
    SENT,

    /**
     * Message delivered to recipient's device (double grey tick ✓✓).
     * Phone was online and received it.
     */
    DELIVERED,

    /**
     * Recipient opened/read the message (blue tick ✓✓).
     * Highest confirmation level.
     */
    READ,

    /**
     * Delivery failed at Meta's side after initial acceptance.
     * e.g. number doesn't exist on WhatsApp, permanent failure.
     */
    FAILED
}
