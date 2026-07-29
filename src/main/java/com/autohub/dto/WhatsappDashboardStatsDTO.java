package com.autohub.dto;

import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.enums.WhatsappMessageStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * All DTOs for WhatsApp dashboard stats.
 * Kept in one file for easy reference — inner static classes.
 */
public class WhatsappDashboardStatsDTO {

    /**
     * Top-level dashboard summary across ALL templates.
     * Used by: GET /api/admin/whatsapp/dashboard
     */
    @Getter
    @Builder
    public static class OverallStats {
        private long totalMessagesSent;       // across all 3 templates
        private long totalDelivered;
        private long totalRead;
        private long totalFailed;
        private long totalAccepted;           // sent but no delivery confirmation yet
        private long totalSent;               // confirmed sent by Meta
        private double overallDeliveryRate;   // delivered / totalSent * 100
        private double overallReadRate;       // read / delivered * 100

        private TemplateStats leadNotifications;
        private TemplateStats offerBroadcasts;
        private TemplateStats vehicleShares;
    }

    /**
     * Stats for one specific template type.
     */
    @Getter
    @Builder
    public static class TemplateStats {
        private String templateType;          // LEAD / OFFER / VEHICLE
        private String templateName;
        private long totalSent;
        private long accepted;
        private long sent;
        private long delivered;
        private long read;
        private long failed;
        private long inQueue;                 // accepted but not yet sent
        private double deliveryRate;
        private double readRate;
        private LocalDateTime lastSentAt;
    }

    /**
     * Single failed message record — shown in failed messages table.
     * Used by: GET /api/admin/whatsapp/failed-messages
     */
    @Getter
    @Builder
    public static class FailedMessageDTO {
        private Long logId;
        private String logType;               // LEAD / OFFER / VEHICLE
        private Long referenceId;             // leadId / offerId / vehicleId
        private Long dealerId;
        private String dealerName;
        private String mobileNumber;
        private String templateName;
        private WhatsappMessageStatus apiStatus;      // SUCCESS/FAILED (API call result)
        private WhatsappDeliveryStatus deliveryStatus; // ACCEPTED/SENT/DELIVERED/READ/FAILED
        private String errorMessage;
        private String responsePayload;
        private int retryCount;
        private LocalDateTime createdAt;
        private LocalDateTime lastRetryAt;
        private boolean canRetry;             // false if retryCount >= 3
    }

    /**
     * Result of a retry attempt.
     * Used by: POST /api/admin/whatsapp/retry/{logType}/{logId}
     */
    @Getter
    @Builder
    public static class RetryResultDTO {
        private Long logId;
        private String logType;
        private boolean success;
        private String whatsappMessageId;
        private int retryCount;
        private String message;
    }

    /**
     * Complete broadcast summary for one offer.
     * Used by: GET /api/admin/whatsapp/offers/{offerId}/delivery-summary
     */
    @Getter
    @Builder
    public static class OfferDeliverySummaryDTO {
        private Long offerId;
        private String offerTitle;
        private int totalDealers;
        private long accepted;
        private long sent;
        private long delivered;
        private long read;
        private long failed;
        private double deliveryRate;
        private List<DealerDeliveryStatus> dealerBreakdown;
    }

    /**
     * Per-dealer delivery status inside an offer summary.
     */
    @Getter
    @Builder
    public static class DealerDeliveryStatus {
        private Long dealerId;
        private String dealerName;
        private String mobileNumber;
        private WhatsappDeliveryStatus deliveryStatus;
        private String whatsappMessageId;
        private LocalDateTime sentAt;
    }
}