package com.autohub.service;

import com.autohub.dto.LeadCreatedEvent;

public interface WhatsAppNotificationService {
    void notifyDealerOfNewLead(LeadCreatedEvent event);
}