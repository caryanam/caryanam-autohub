package com.autohub.entity;

import com.autohub.enums.WhatsappMessageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Dedicated audit log for every WhatsApp offer broadcast message sent to a dealer.
 * One row per dealer per offer broadcast.
 * Mirrors WhatsappMessageLog but scoped to offer broadcasts — keeps concerns separated
 * and makes it easy to query "which dealers received offer X" vs "which dealers got a lead alert".
 */
@Entity
@Table(name = "whatsapp_offer_message_log", indexes = {
        @Index(name = "idx_offer_log_offer_id", columnList = "offer_id"),
        @Index(name = "idx_offer_log_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_offer_log_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappOfferMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

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

    @Column(name = "meta_image_handle", length = 200)
    private String metaImageHandle;

    @Lob
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}