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

/**
 * Core business logic for dispatching the "New Lead Generated" WhatsApp notification
 * and persisting an audit log entry regardless of outcome.
 *
 * IMPORTANT: this runs on the async executor, AFTER the lead transaction has already
 * committed. Any exception here must NEVER be allowed to affect the lead record -
 * we use a fresh, independent transaction (REQUIRES_NEW) purely for writing the log row.
 */
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

        // ---- Validation block (Requirement #20) ----
        try {
            validate(event);
        } catch (InvalidDealerContactException ex) {
            log.error("Validation failed for leadId [{}]: {}", event.leadId(), ex.getMessage());
            persistLog(event, WhatsappMessageStatus.FAILED, null, "{}", "{\"error\":\"" + ex.getMessage() + "\"}");
            return; // do not attempt the API call at all
        }

        String normalizedMobile = normalizeToE164(event.dealerWhatsappNumber());

        WhatsAppTemplateRequest request = WhatsAppTemplateRequest.forNewLead(
                normalizedMobile,
                properties.templateName(),
                properties.languageCode(),
                event.customerName(),
                event.customerMobile(),
                event.vehicleDisplayName()
        );

        try {
            WhatsAppClient.WhatsAppApiCallResult result = whatsAppClient.sendTemplateMessage(request);

            if (result.success()) {
                persistLog(event, WhatsappMessageStatus.SUCCESS, result.whatsappMessageId(),
                        result.requestPayload(), result.responsePayload());
                log.info("WhatsApp notification SUCCESS for leadId [{}], dealerId [{}], messageId [{}]",
                        event.leadId(), event.dealerId(), result.whatsappMessageId());
            } else {
                persistLog(event, WhatsappMessageStatus.FAILED, null,
                        result.requestPayload(), result.responsePayload());
                log.error("WhatsApp notification FAILED (after retries) for leadId [{}], dealerId [{}]: {}",
                        event.leadId(), event.dealerId(), result.errorMessage());
            }

        } catch (WhatsAppApiException ex) {
            // Permanent (4xx) failure - not retried by RetryTemplate, caught here directly
            persistLog(event, WhatsappMessageStatus.FAILED, null,
                    "{}", ex.getResponseBody() != null ? ex.getResponseBody() : "{\"error\":\"" + ex.getMessage() + "\"}");
            log.error("WhatsApp notification permanently failed for leadId [{}]: {}", event.leadId(), ex.getMessage(), ex);

        } catch (Exception ex) {
            // Absolute last-resort safety net - lead data is already committed and safe
            persistLog(event, WhatsappMessageStatus.FAILED, null, "{}",
                    "{\"error\":\"" + ex.getMessage() + "\"}");
            log.error("Unexpected exception sending WhatsApp notification for leadId [{}]", event.leadId(), ex);
        }
    }

    /** Requirement #20: validate dealer WhatsApp number, lead data, and required template vars. */
    private void validate(LeadCreatedEvent event) {
        if (event == null) {
            throw new InvalidDealerContactException("Lead event payload is null");
        }
        if (!StringUtils.hasText(event.dealerWhatsappNumber())) {
            throw new InvalidDealerContactException(
                    "Dealer [" + event.dealerId() + "] has no WhatsApp number configured");
        }
        if (!StringUtils.hasText(event.customerName())) {
            throw new InvalidDealerContactException("Customer name is empty for leadId [" + event.leadId() + "]");
        }
        if (!StringUtils.hasText(event.customerMobile())) {
            throw new InvalidDealerContactException("Customer mobile is empty for leadId [" + event.leadId() + "]");
        }
        if (!StringUtils.hasText(event.vehicleDisplayName())) {
            throw new InvalidDealerContactException("Vehicle name is empty for leadId [" + event.leadId() + "]");
        }
    }

    /**
     * Normalizes dealer number to E.164-ish digits-only format Meta expects (no '+', no spaces).
     * Adjust country-code prefixing logic to match how numbers are actually stored in your DB.
     */
    private String normalizeToE164(String rawNumber) {
        String digitsOnly = rawNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            // Assume Indian numbers stored without country code - prefix 91
            return "91" + digitsOnly;
        }
        return digitsOnly;
    }

    /**
     * Persists the audit log in its OWN independent transaction.
     * REQUIRES_NEW guarantees this write commits/rollbacks completely independently
     * of anything else - per requirement #8, nothing here can ever roll back the lead.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistLog(LeadCreatedEvent event, WhatsappMessageStatus status, String messageId,
                           String requestPayload, String responsePayload) {
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
            // Even logging failures must not propagate - log to application logs as last resort
            log.error("Failed to persist WhatsappMessageLog for leadId [{}]", event.leadId(), ex);
        }
    }
}
