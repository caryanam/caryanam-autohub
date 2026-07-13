package com.autohub.dto;
import com.autohub.enums.WhatsappMessageStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DealerOfferResponseDTO {

    // ── Used for both single send response AND list view ──
    private Long offerId;
    private String offerTitle;
    private String dealerGreetingName;
    private String offerDetails;
    private String benefits;
    private String contactInfo;
    private String imageUrl;
    private String status;
    private Integer totalDealersTargeted;
    private Integer totalSentSuccess;
    private Integer totalSentFailed;
    private LocalDateTime createdAt;
    private String message;

    // ── Only populated in list view (per-dealer breakdown) ──
    private List<DealerLogEntry> dealerLogs;

    @Builder
    @Getter
    public static class DealerLogEntry {
        private Long dealerId;
        private String dealerName;
        private String mobileNumber;
        private WhatsappMessageStatus status;
        private String whatsappMessageId;
        private String errorMessage;
        private LocalDateTime sentAt;
    }
}