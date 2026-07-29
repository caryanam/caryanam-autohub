package com.autohub.service;

import com.autohub.dto.WhatsappDashboardStatsDTO;

public interface WhatsappRetryService {

    /**
     * Retry a single failed message by log type and ID.
     * logType = "LEAD", "OFFER", or "VEHICLE"
     * Max 3 retries per message — throws if already at limit.
     */
    WhatsappDashboardStatsDTO.RetryResultDTO retryFailedMessage(String logType, Long logId);
}