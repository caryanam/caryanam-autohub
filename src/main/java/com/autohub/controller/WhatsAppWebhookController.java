package com.autohub.controller;

import com.autohub.service.WhatsAppWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives Meta WhatsApp webhook callbacks.
 *
 * Two endpoints:
 *  GET  /api/webhook/whatsapp → webhook verification (called once by Meta on setup)
 *  POST /api/webhook/whatsapp → delivery status updates (called on every status change)
 *
 * IMPORTANT: These endpoints must be publicly accessible (no JWT auth).
 * Meta's servers call these from their IPs — they cannot send a JWT token.
 * Add both paths to your Spring Security permit list.
 */
@RestController
@RequestMapping("/api/webhook/whatsapp")
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(WhatsAppWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Meta calls this GET endpoint once when you register the webhook URL
     * in the developer dashboard, to confirm your server is reachable.
     *
     * Meta sends:
     *   ?hub.mode=subscribe
     *   &hub.verify_token=YOUR_SECRET_TOKEN
     *   &hub.challenge=RANDOM_NUMBER
     *
     * We must return the challenge number as plain text (200 OK)
     * if the token matches, otherwise Meta won't register the webhook.
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode")         String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge")    String challenge) {

        log.info("Webhook verification request received");
        String response = webhookService.verifyWebhook(mode, token, challenge);

        if ("VERIFICATION_FAILED".equals(response)) {
            return ResponseEntity.status(403).body("VERIFICATION_FAILED");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Meta calls this POST endpoint for every status update:
     * sent → delivered → read (or failed).
     *
     * Must return 200 OK quickly — if we return anything else or
     * take too long, Meta will retry the webhook multiple times,
     * causing duplicate processing. We always return 200 and
     * process asynchronously.
     */
    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody JsonNode payload) {
        log.info("Webhook POST received");

        // Process asynchronously — return 200 immediately
        // so Meta doesn't retry thinking we failed
        webhookService.processWebhookPayload(payload);

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
