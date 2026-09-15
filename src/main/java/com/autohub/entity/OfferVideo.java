package com.autohub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores promotional offer videos uploaded by the admin.
 * Only ONE video can be enabled (active) at a time — shown on the public homepage.
 * When a new video is enabled, all others are automatically disabled.
 */
@Entity
@Table(name = "offer_videos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(name = "video_url", nullable = false)
    private String videoUrl;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
