package com.autohub.controller;

import com.autohub.service.WhatsAppWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Receives Meta WhatsApp webhook callbacks.
 *
 * Three endpoints:
 *  GET  /api/webhook/whatsapp             → webhook verification (called once by Meta on setup)
 *  POST /api/webhook/whatsapp             → delivery status updates (called on every status change)
 *  GET  /api/webhook/whatsapp/test-status → diagnostic endpoint to verify reachability
 *
 * IMPORTANT: These endpoints must be publicly accessible (no JWT auth).
 * Meta's servers call these from their IPs — they cannot send a JWT token.
 * Add both paths to your Spring Security permit list.
 *
 * NOTE: The POST endpoint accepts raw String instead of JsonNode to avoid
 * Jackson 3.x type-definition errors caused by jjwt-jackson 0.11.5
 * pulling in Jackson 2.x classes alongside Spring Boot 4.x's Jackson 3.x.
 */
@RestController
@RequestMapping("/api/webhook/whatsapp")
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(WhatsAppWebhookService webhookService,
                                     ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
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
     * process synchronously (fast enough for our use case).
     *
     * IMPORTANT: Accepts raw String body (not JsonNode) to avoid
     * Jackson version conflict. Manually parsed to JsonNode using
     * Spring's auto-configured ObjectMapper.
     */
    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody String rawPayload) {
        log.info("═══════════════════════════════════════════════════");
        log.info("WEBHOOK POST RECEIVED — raw payload: {}", rawPayload);
        log.info("═══════════════════════════════════════════════════");

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            webhookService.processWebhookPayload(payload);
        } catch (Exception ex) {
            // Always return 200 — never let Meta retry due to our parsing failures
            log.error("Failed to parse webhook payload: {}", ex.getMessage(), ex);
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Diagnostic endpoint to verify webhook reachability.
     * Call this from browser or curl to confirm:
     *   1. The server is running
     *   2. The webhook path is not blocked by JWT/security
     *   3. The reverse proxy (Nginx) is forwarding correctly
     *
     * Usage: GET https://c1.caryanam.com/api/webhook/whatsapp/test-status
     *
     * This endpoint can be removed once webhook delivery is confirmed working.
     */
    @GetMapping("/test-status")
    public ResponseEntity<Map<String, Object>> testWebhookStatus() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "ACTIVE");
        info.put("endpoint", "/api/webhook/whatsapp");
        info.put("message", "Webhook endpoint is reachable — no JWT/security blocking");
        info.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(info);
    }
}
