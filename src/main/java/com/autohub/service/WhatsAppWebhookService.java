package com.autohub.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface WhatsAppWebhookService {

    /**
     * Handles the webhook verification GET request from Meta.
     * Meta sends this once when you register the webhook URL
     * in the developer dashboard to confirm your server is live.
     */
    String verifyWebhook(String mode, String token, String challenge);

    /**
     * Processes incoming webhook POST payload from Meta.
     * Contains message status updates: sent, delivered, read, failed.
     */
    void processWebhookPayload(JsonNode payload);
}