package com.autohub.serviceImpl;

import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.repository.WhatsappMessageLogRepository;
import com.autohub.repository.WhatsappOfferMessageLogRepository;
import com.autohub.repository.WhatsappVehicleShareLogRepository;
import com.autohub.service.WhatsAppWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class WhatsAppWebhookServiceImpl implements WhatsAppWebhookService {

    /**
     * This token is a secret string YOU define — it must match
     * exactly what you enter in the Meta developer dashboard
     * when registering the webhook URL.
     * Add to application.properties:
     *   whatsapp.webhook-verify-token=caryanam_webhook_secret_2026
     */
    @Value("${whatsapp.webhook-verify-token}")
    private String webhookVerifyToken;

    private final WhatsappMessageLogRepository messageLogRepository;
    private final WhatsappOfferMessageLogRepository offerMessageLogRepository;
    private final WhatsappVehicleShareLogRepository vehicleShareLogRepository;

    public WhatsAppWebhookServiceImpl(
            WhatsappMessageLogRepository messageLogRepository,
            WhatsappOfferMessageLogRepository offerMessageLogRepository,
            WhatsappVehicleShareLogRepository vehicleShareLogRepository) {
        this.messageLogRepository = messageLogRepository;
        this.offerMessageLogRepository = offerMessageLogRepository;
        this.vehicleShareLogRepository = vehicleShareLogRepository;
    }

    /**
     * Meta calls GET /api/webhook/whatsapp?hub.mode=subscribe
     *   &hub.verify_token=YOUR_TOKEN&hub.challenge=RANDOM_NUMBER
     *
     * We must return the challenge string if token matches,
     * otherwise return "VERIFICATION_FAILED".
     */
    @Override
    public String verifyWebhook(String mode, String token, String challenge) {
        log.info("Webhook verification attempt → mode=[{}] token=[{}]", mode, token);

        if ("subscribe".equals(mode) && webhookVerifyToken.equals(token)) {
            log.info("Webhook verified successfully");
            return challenge;
        }

        log.warn("Webhook verification FAILED — token mismatch. " +
                "Expected=[{}] Received=[{}]", webhookVerifyToken, token);
        return "VERIFICATION_FAILED";
    }

    /**
     * Meta sends POST with this structure for status updates:
     * {
     *   "object": "whatsapp_business_account",
     *   "entry": [{
     *     "changes": [{
     *       "value": {
     *         "statuses": [{
     *           "id": "wamid.xxx",
     *           "status": "sent|delivered|read|failed",
     *           "timestamp": "1234567890",
     *           "recipient_id": "919876543210",
     *           "errors": [...]   (only on failed)
     *         }]
     *       }
     *     }]
     *   }]
     * }
     */
    @Override
    @Transactional
    public void processWebhookPayload(JsonNode payload) {
        try {
            // Validate this is a WhatsApp business account event
            String object = payload.path("object").asText();
            if (!"whatsapp_business_account".equals(object)) {
                log.debug("Ignoring non-WhatsApp webhook event: object=[{}]", object);
                return;
            }

            JsonNode entries = payload.path("entry");
            if (entries.isMissingNode() || !entries.isArray()) {
                log.warn("Webhook payload missing 'entry' array");
                return;
            }

            // Iterate through all entries and changes
            for (JsonNode entry : entries) {
                for (JsonNode change : entry.path("changes")) {

                    JsonNode value = change.path("value");

                    // Process message status updates
                    JsonNode statuses = value.path("statuses");
                    if (statuses.isArray() && !statuses.isEmpty()) {
                        for (JsonNode statusNode : statuses) {
                            processStatusUpdate(statusNode);
                        }
                    }

                    // Process incoming messages (optional — log them)
                    JsonNode messages = value.path("messages");
                    if (messages.isArray() && !messages.isEmpty()) {
                        for (JsonNode message : messages) {
                            log.info("Incoming message received from [{}] — type=[{}]",
                                    message.path("from").asText(),
                                    message.path("type").asText());
                        }
                    }
                }
            }

        } catch (Exception ex) {
            // Never throw from webhook handler — Meta will retry
            // if we return non-200, causing duplicate processing
            log.error("Error processing webhook payload: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Processes a single status update node and updates
     * the corresponding log entry across all three log tables.
     */
    private void processStatusUpdate(JsonNode statusNode) {

        String wamid       = statusNode.path("id").asText();
        String statusStr   = statusNode.path("status").asText();
        String recipientId = statusNode.path("recipient_id").asText();
        String timestamp   = statusNode.path("timestamp").asText();

        if (wamid == null || wamid.isBlank()) {
            log.warn("Status update missing wamid — skipping");
            return;
        }

        // Convert Meta's status string to our enum
        WhatsappDeliveryStatus deliveryStatus = mapMetaStatus(statusStr);

        log.info("Status update → wamid=[{}] status=[{}] recipient=[{}] timestamp=[{}]",
                wamid, statusStr, recipientId, timestamp);

        // Extract error details if present (for FAILED status)
        String errorDetails = null;
        JsonNode errors = statusNode.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            JsonNode firstError = errors.get(0);
            errorDetails = "Code: " + firstError.path("code").asText()
                    + " — " + firstError.path("title").asText();
            log.error("Delivery failure for wamid=[{}]: {}", wamid, errorDetails);
        }

        // ── Update all three log tables by wamid ──
        // We search all three because the same wamid could be in any table.
        // In practice only one table will have a match.

        boolean updated = false;
        final String finalErrorDetails = errorDetails;

        // 1. Lead notification log
        var leadLogs = messageLogRepository.findByWhatsappMessageId(wamid);
        if (!leadLogs.isEmpty()) {
            leadLogs.forEach(log -> {
                log.setDeliveryStatus(deliveryStatus);
                if (finalErrorDetails != null) {
                    log.setResponsePayload(
                            log.getResponsePayload() + " | DELIVERY_ERROR: " + finalErrorDetails);
                }
            });
            messageLogRepository.saveAll(leadLogs);
            log.info("Updated {} lead log(s) for wamid=[{}] → status=[{}]",
                    leadLogs.size(), wamid, deliveryStatus);
            updated = true;
        }

        // 2. Offer broadcast log
        var offerLogs = offerMessageLogRepository.findByWhatsappMessageId(wamid);
        if (!offerLogs.isEmpty()) {
            offerLogs.forEach(log -> {
                log.setDeliveryStatus(deliveryStatus);
                if (finalErrorDetails != null) {
                    log.setErrorMessage(finalErrorDetails);
                }
            });
            offerMessageLogRepository.saveAll(offerLogs);
            log.info("Updated {} offer log(s) for wamid=[{}] → status=[{}]",
                    offerLogs.size(), wamid, deliveryStatus);
            updated = true;
        }

        // 3. Vehicle share log
        var vehicleLogs = vehicleShareLogRepository.findByWhatsappMessageId(wamid);
        if (!vehicleLogs.isEmpty()) {
            vehicleLogs.forEach(log -> {
                log.setDeliveryStatus(deliveryStatus);
                if (finalErrorDetails != null) {
                    log.setErrorMessage(finalErrorDetails);
                }
            });
            vehicleShareLogRepository.saveAll(vehicleLogs);
            log.info("Updated {} vehicle share log(s) for wamid=[{}] → status=[{}]",
                    vehicleLogs.size(), wamid, deliveryStatus);
            updated = true;
        }

        if (!updated) {
            // This can happen if webhook fires before our DB write completes
            // (race condition on very fast delivery) — safe to ignore
            log.warn("No log found for wamid=[{}] — may have arrived before DB write",
                    wamid);
        }
    }

    /**
     * Maps Meta's webhook status strings to our internal enum.
     * Meta sends: "sent", "delivered", "read", "failed"
     */
    private WhatsappDeliveryStatus mapMetaStatus(String metaStatus) {
        return switch (metaStatus.toLowerCase()) {
            case "sent"      -> WhatsappDeliveryStatus.SENT;
            case "delivered" -> WhatsappDeliveryStatus.DELIVERED;
            case "read"      -> WhatsappDeliveryStatus.READ;
            case "failed"    -> WhatsappDeliveryStatus.FAILED;
            default -> {
                log.warn("Unknown Meta delivery status: '{}' — defaulting to ACCEPTED",
                        metaStatus);
                yield WhatsappDeliveryStatus.ACCEPTED;
            }
        };
    }
}
