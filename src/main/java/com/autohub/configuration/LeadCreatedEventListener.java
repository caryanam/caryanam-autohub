package com.autohub.configuration;

import com.autohub.dto.LeadCreatedEvent;
import com.autohub.service.WhatsAppNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.springframework.stereotype.Component;

/**
 * Listens for LeadCreatedEvent and triggers WhatsApp notification dispatch.
 *
 * Two critical annotations work together here:
 *  - @TransactionalEventListener(phase = AFTER_COMMIT): guarantees this only fires
 *    if and after the lead-saving transaction has actually committed to MySQL.
 *    If the transaction rolls back, this listener NEVER runs - so we never notify
 *    a dealer about a lead that doesn't actually exist in the DB.
 *  - @Async: ensures the listener executes on a separate thread pool (whatsAppTaskExecutor),
 *    so the original HTTP request thread is NOT blocked waiting for Meta's API,
 *    and a slow/down WhatsApp API can never slow down lead creation responses.
 */
@Component
@Slf4j
public class LeadCreatedEventListener {

    private final WhatsAppNotificationService whatsAppNotificationService;

    public LeadCreatedEventListener(WhatsAppNotificationService whatsAppNotificationService) {
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    @Async("whatsAppTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLeadCreated(LeadCreatedEvent event) {
        log.info("AFTER_COMMIT triggered for leadId [{}] - dispatching WhatsApp notification", event.leadId());

        try {
            whatsAppNotificationService.notifyDealerOfNewLead(event);
        } catch (Exception ex) {
            // Defensive catch-all: WhatsAppNotificationServiceImpl already handles its own
            // exceptions and persists failure logs internally. This is a last-resort guard
            // so that NOTHING from this listener can ever propagate and affect anything else -
            // the lead is already committed and safe regardless of what happens here.
            log.error("Unexpected error while processing WhatsApp notification for leadId [{}]: {}",
                    event.leadId(), ex.getMessage(), ex);
        }
    }
}
