package com.autohub.entity;

import com.autohub.enums.WhatsappMessageStatus;
import jakarta.persistence.*;
import lombok.*;
import com.autohub.enums.WhatsappDeliveryStatus;

import java.time.LocalDateTime;

/**
 * Audit log for every vehicle detail share sent via WhatsApp.
 * One row per share attempt — dealer can share the same vehicle
 * multiple times, each gets its own log row.
 */
@Entity
@Table(name = "whatsapp_vehicle_share_log", indexes = {
        @Index(name = "idx_vs_log_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_vs_log_dealer_id",  columnList = "dealer_id"),
        @Index(name = "idx_vs_log_status",      columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappVehicleShareLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "dealer_id", nullable = false)
    private Long dealerId;

    @Column(name = "dealer_name", length = 100)
    private String dealerName;

    // Dealer's own WhatsApp number — this is the recipient
    @Column(name = "sent_to_number", nullable = false, length = 20)
    private String sentToNumber;

    @Column(name = "vehicle_display_name", length = 200)
    private String vehicleDisplayName;

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

    @Column(name = "shared_at", nullable = false, updatable = false)
    private LocalDateTime sharedAt;

    // Add this field inside the entity class
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private WhatsappDeliveryStatus deliveryStatus;

    @PrePersist
    public void prePersist() {
        if (sharedAt == null) {
            sharedAt = LocalDateTime.now();
        }

        // Default delivery status on first save
        if (deliveryStatus == null) {
            deliveryStatus = WhatsappDeliveryStatus.ACCEPTED;
        }
    }

}