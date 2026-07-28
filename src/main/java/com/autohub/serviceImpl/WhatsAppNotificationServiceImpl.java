package com.autohub.serviceImpl;

import com.autohub.configuration.WhatsAppClient;
import com.autohub.dto.LeadCreatedEvent;
import com.autohub.dto.WhatsAppProperties;
import com.autohub.dto.WhatsAppTemplateRequest;
import com.autohub.entity.WhatsappMessageLog;
import com.autohub.enums.WhatsappMessageStatus;
import com.autohub.exception.InvalidDealerContactException;
import com.autohub.exception.WhatsAppApiException;
import com.autohub.repository.WhatsappMessageLogRepository;
import com.autohub.service.WhatsAppNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class WhatsAppNotificationServiceImpl implements WhatsAppNotificationService {

    private final WhatsAppClient whatsAppClient;
    private final WhatsAppProperties properties;
    private final WhatsappMessageLogRepository messageLogRepository;

    public WhatsAppNotificationServiceImpl(WhatsAppClient whatsAppClient,
                                           WhatsAppProperties properties,
                                           WhatsappMessageLogRepository messageLogRepository) {
        this.whatsAppClient = whatsAppClient;
        this.properties = properties;
        this.messageLogRepository = messageLogRepository;
    }

    @Override
    public void notifyDealerOfNewLead(LeadCreatedEvent event) {

        // ── Validation ──
        try {
            validate(event);
        } catch (InvalidDealerContactException ex) {
            log.error("Validation failed for leadId [{}]: {}", event.leadId(), ex.getMessage());
            persistLog(event, WhatsappMessageStatus.FAILED, null,
                    "{}", "{\"error\":\"" + ex.getMessage() + "\"}");
            return;
        }

        // ── FIX: Normalize dealer number with bulletproof logic ──
        String normalizedMobile = normalizeToE164(event.dealerWhatsappNumber());

        // If normalization fails, log and abort — don't send garbage to Meta
        if (normalizedMobile == null) {
            log.error("Cannot normalize dealer WhatsApp number for leadId [{}]: '{}'",
                    event.leadId(), event.dealerWhatsappNumber());
            persistLog(event, WhatsappMessageStatus.FAILED, null,
                    "{}",
                    "{\"error\":\"Invalid WhatsApp number format: '"
                            + event.dealerWhatsappNumber() + "'\"}");
            return;
        }

        log.info("Normalized dealer number: '{}' → '{}'",
                event.dealerWhatsappNumber(), normalizedMobile);

        WhatsAppTemplateRequest request = WhatsAppTemplateRequest.forNewLead(
                normalizedMobile,
                properties.templateName(),
                properties.languageCode(),
                event.customerName(),
                event.customerMobile(),
                event.vehicleDisplayName()
        );

        try {
            WhatsAppClient.WhatsAppApiCallResult result =
                    whatsAppClient.sendTemplateMessage(request);

            if (result.success()) {
                persistLog(event, WhatsappMessageStatus.SUCCESS,
                        result.whatsappMessageId(),
                        result.requestPayload(),
                        result.responsePayload());
                log.info("WhatsApp notification SUCCESS for leadId [{}], " +
                                "dealerId [{}], messageId [{}]",
                        event.leadId(), event.dealerId(), result.whatsappMessageId());
            } else {
                persistLog(event, WhatsappMessageStatus.FAILED, null,
                        result.requestPayload(), result.responsePayload());
                log.error("WhatsApp notification FAILED (after retries) for " +
                                "leadId [{}], dealerId [{}]: {}",
                        event.leadId(), event.dealerId(), result.errorMessage());
            }

        } catch (WhatsAppApiException ex) {
            // Permanent 4xx failure — not retried, caught here directly
            persistLog(event, WhatsappMessageStatus.FAILED, null,
                    "{}",
                    ex.getResponseBody() != null
                            ? ex.getResponseBody()
                            : "{\"error\":\"" + ex.getMessage() + "\"}");
            log.error("WhatsApp notification permanently failed for leadId [{}]: {}",
                    event.leadId(), ex.getMessage(), ex);

        } catch (Exception ex) {
            // Absolute last-resort safety net — lead data is already committed and safe
            persistLog(event, WhatsappMessageStatus.FAILED, null,
                    "{}",
                    "{\"error\":\"" + ex.getMessage() + "\"}");
            log.error("Unexpected exception sending WhatsApp notification for leadId [{}]",
                    event.leadId(), ex);
        }
    }

    /**
     * Validates all required fields before attempting the API call.
     * Throws InvalidDealerContactException on any missing/blank field.
     */
    private void validate(LeadCreatedEvent event) {
        if (event == null) {
            throw new InvalidDealerContactException("Lead event payload is null");
        }
        if (!StringUtils.hasText(event.dealerWhatsappNumber())) {
            throw new InvalidDealerContactException(
                    "Dealer [" + event.dealerId() + "] has no WhatsApp number configured");
        }
        if (!StringUtils.hasText(event.customerName())) {
            throw new InvalidDealerContactException(
                    "Customer name is empty for leadId [" + event.leadId() + "]");
        }
        if (!StringUtils.hasText(event.customerMobile())) {
            throw new InvalidDealerContactException(
                    "Customer mobile is empty for leadId [" + event.leadId() + "]");
        }
        if (!StringUtils.hasText(event.vehicleDisplayName())) {
            throw new InvalidDealerContactException(
                    "Vehicle name is empty for leadId [" + event.leadId() + "]");
        }
    }

    /**
     * Converts any real-world Indian mobile number format to the
     * E.164 digits-only format Meta requires (e.g. 919876543210).
     *
     * Handles all formats found in practice:
     *   9876543210        → 919876543210   (10 digit clean)
     *   919876543210      → 919876543210   (already correct 12 digit)
     *   +919876543210     → 919876543210   (+ prefix)
     *   +91 9876543210    → 919876543210   (+ and space)
     *   09876543210       → 919876543210   (leading 0 — ISD habit)
     *   0091 9876543210   → 919876543210   (0091 prefix)
     *   91 98765 43210    → 919876543210   (formatted with spaces)
     *   910 9876543210    → 919876543210   (910 prefix edge case)
     *
     * Returns null if the number cannot be normalized to a valid
     * 12-digit Indian mobile number — caller must handle null.
     */
    private String normalizeToE164(String rawNumber) {

        if (rawNumber == null || rawNumber.isBlank()) {
            return null;
        }

        // Strip everything except digits
        String digits = rawNumber.replaceAll("[^0-9]", "");

        // Already correct: 91 + 10 digit Indian number = 12 digits
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits;
        }

        // Clean 10-digit Indian mobile number — just prefix 91
        if (digits.length() == 10) {
            return "91" + digits;
        }

        // 11 digits with leading 0 (e.g. 09876543210) — strip 0, prefix 91
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }

        // 13 digits with 0091 prefix (e.g. 00919876543210) — strip 00, keep rest
        if (digits.length() == 13 && digits.startsWith("0091")) {
            return digits.substring(2);
        }

        // 13 digits starting with 910 — country code + leading 0 edge case
        if (digits.length() == 13 && digits.startsWith("910")) {
            return "91" + digits.substring(3);
        }

        // Unrecognized format — return null so caller handles it cleanly
        log.warn("Cannot normalize WhatsApp number to E164: '{}' → " +
                        "digits='{}' length={}",
                rawNumber, digits, digits.length());
        return null;
    }

    /**
     * Persists the audit log in its OWN independent transaction.
     * REQUIRES_NEW guarantees this write commits/rollbacks completely independently
     * of anything else — nothing here can ever roll back the lead.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistLog(LeadCreatedEvent event,
                           WhatsappMessageStatus status,
                           String messageId,
                           String requestPayload,
                           String responsePayload) {
        try {
            WhatsappMessageLog logEntry = WhatsappMessageLog.builder()
                    .leadId(event.leadId())
                    .dealerId(event.dealerId())
                    .mobileNumber(event.dealerWhatsappNumber())
                    .templateName(properties.templateName())
                    .status(status)
                    .whatsappMessageId(messageId)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .build();

            messageLogRepository.save(logEntry);
        } catch (Exception ex) {
            // Even logging failures must not propagate
            log.error("Failed to persist WhatsappMessageLog for leadId [{}]",
                    event.leadId(), ex);
        }
    }
}