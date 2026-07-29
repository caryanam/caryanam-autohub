package com.autohub.entity;

import com.autohub.enums.WhatsappMessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.autohub.enums.WhatsappDeliveryStatus;


import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_message_log", indexes = {
        @Index(name = "idx_wa_log_lead_id", columnList = "lead_id"),
        @Index(name = "idx_wa_log_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_wa_log_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Column(name = "dealer_id", nullable = false)
    private Long dealerId;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WhatsappMessageStatus status;

    @Column(name = "whatsapp_message_id", length = 100)
    private String whatsappMessageId;

    // LONGTEXT/CLOB-style storage for full request/response audit - useful for Meta support tickets
    @Lob
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    // New fields — add inside the class body
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private WhatsappDeliveryStatus deliveryStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;


    @Lob
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
//
//    @PrePersist
//    public void prePersist() {
//        if (createdAt == null) {
//            createdAt = LocalDateTime.now();
//        }
//    }



    // Update @PrePersist to set default deliveryStatus
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (deliveryStatus == null) {
            deliveryStatus = WhatsappDeliveryStatus.ACCEPTED;
        }
        if (retryCount < 0) {
            retryCount = 0;
        }
    }
}
