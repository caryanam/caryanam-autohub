package com.autohub.entity;

import com.autohub.enums.SocialPostPublishStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One row per vehicle inside a {@link SocialPostBatch}. Mirrors the
 * publish-status snapshot of the linked {@link VehicleSocialPostRequest}
 * for this specific batch run, so batch-level progress can be queried
 * without recomputing from request history (a request may appear in more
 * than one batch across retries over time).
 */
@Entity
@Table(name = "social_post_batch_items", indexes = {
        @Index(name = "idx_spbi_batch_id", columnList = "batch_id"),
        @Index(name = "idx_spbi_request_id", columnList = "request_id"),
        @Index(name = "idx_spbi_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_spbi_batch_request", columnNames = {"batch_id", "request_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPostBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, foreignKey = @ForeignKey(name = "fk_spbi_batch"))
    private SocialPostBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, foreignKey = @ForeignKey(name = "fk_spbi_request"))
    private VehicleSocialPostRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_spbi_vehicle"))
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SocialPostPublishStatus status = SocialPostPublishStatus.QUEUED;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "retryable", nullable = false)
    @Builder.Default
    private Boolean retryable = true;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
