package com.autohub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores every offer broadcast the admin sends.
 * image_url = local server path where the uploaded file is saved.
 * meta_image_handle = the media ID returned by Meta after uploading
 * the image to their servers — reused for all dealer sends in this broadcast.
 */
@Entity
@Table(name = "dealer_offers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealerOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_title", nullable = false, length = 200)
    private String offerTitle;

    @Column(name = "dealer_greeting_name", nullable = false, length = 100)
    private String dealerGreetingName; // {{1}} - e.g. "Valued Partner"

    @Column(name = "offer_details", nullable = false, columnDefinition = "TEXT")
    private String offerDetails; // {{2}}

    @Column(name = "benefits", nullable = false, columnDefinition = "TEXT")
    private String benefits; // {{3}}

    @Column(name = "contact_info", nullable = false, length = 100)
    private String contactInfo; // {{4}}

    @Column(name = "image_url", nullable = false)
    private String imageUrl; // local saved file path

    @Column(name = "meta_image_handle", length = 200)
    private String metaImageHandle; // returned by Meta media upload API

    @Column(name = "total_dealers_targeted")
    private Integer totalDealersTargeted;

    @Column(name = "total_sent_success")
    private Integer totalSentSuccess;

    @Column(name = "total_sent_failed")
    private Integer totalSentFailed;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (totalSentSuccess == null) totalSentSuccess = 0;
        if (totalSentFailed == null) totalSentFailed = 0;
    }
}
