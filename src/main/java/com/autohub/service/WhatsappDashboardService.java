package com.autohub.service;

import com.autohub.dto.WhatsappDashboardStatsDTO;

import java.util.List;

public interface WhatsappDashboardService {

    // Overall stats across all templates
    WhatsappDashboardStatsDTO.OverallStats getOverallStats();

    // Per-template stats
    WhatsappDashboardStatsDTO.TemplateStats getLeadNotificationStats();
    WhatsappDashboardStatsDTO.TemplateStats getOfferBroadcastStats();
    WhatsappDashboardStatsDTO.TemplateStats getVehicleShareStats();

    // All failed messages across all templates
    List<WhatsappDashboardStatsDTO.FailedMessageDTO> getAllFailedMessages();

    // Offer-specific delivery breakdown
    WhatsappDashboardStatsDTO.OfferDeliverySummaryDTO getOfferDeliverySummary(Long offerId);

    // Raw logs for frontend tables
    List<com.autohub.entity.WhatsappMessageLog> getLeadLogs();
    List<com.autohub.entity.WhatsappOfferMessageLog> getOfferLogs();
    List<com.autohub.entity.WhatsappVehicleShareLog> getVehicleLogs();
}