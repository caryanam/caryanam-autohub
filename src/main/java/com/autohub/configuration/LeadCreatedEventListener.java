package com.autohub.configuration;

import com.autohub.dto.LeadCreatedEvent;
import com.autohub.service.WhatsAppNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.springframework.stereotype.Component;


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
            log.error("Unexpected error while processing WhatsApp notification for leadId [{}]: {}",
                    event.leadId(), ex.getMessage(), ex);
        }
    }
}
