package com.autohub.service;

import com.autohub.dto.LeadCreatedEvent;

public interface WhatsAppNotificationService {
    void notifyDealerOfNewLead(LeadCreatedEvent event);

    /**
     * Sends a WhatsApp OTP to the dealer's WhatsApp number during registration
     * using the otp_caryanam_verification authentication template.
     */
    void sendDealerRegistrationOtp(String whatsappNumber, String otp);
}