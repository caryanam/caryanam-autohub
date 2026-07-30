package com.autohub.entity;

import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.enums.WhatsappMessageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_birthday_message_log", indexes = {
        @Index(name = "idx_birthday_log_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_birthday_log_status", columnList = "status"),
        @Index(name = "idx_birthday_log_delivery", columnList = "delivery_status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappBirthdayMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dealer_id", nullable = false)
    private Long dealerId;

    @Column(name = "dealer_name", length = 100)
    private String dealerName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WhatsappMessageStatus status;

    @Column(name = "whatsapp_message_id", length = 150)
    private String whatsappMessageId;

    @Lob
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private WhatsappDeliveryStatus deliveryStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

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
