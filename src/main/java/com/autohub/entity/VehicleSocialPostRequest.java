package com.autohub.entity;

import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostPublishStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One row per "dealer requests this vehicle be posted to the Facebook Page".
 * Carries both the approval lifecycle (dealer request -> admin decision) and
 * the publishing lifecycle (queued -> processing -> published/failed).
 * <p>
 * A vehicle may accumulate multiple historical rows over time (e.g. a failed
 * request that was later re-requested), so there is intentionally no unique
 * DB constraint on vehicle_id. Duplicate-active-request protection
 * (no second PENDING/QUEUED/PROCESSING/PUBLISHED row for the same vehicle)
 * is enforced in the service layer - see FacebookPostRequestService.
 */
@Entity
@Table(name = "vehicle_social_post_requests", indexes = {
        @Index(name = "idx_vspr_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_vspr_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_vspr_approval_status", columnList = "approval_status"),
        @Index(name = "idx_vspr_publish_status", columnList = "publish_status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSocialPostRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vspr_vehicle"))
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vspr_dealer"))
    private Dealer dealer;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private SocialPostApprovalStatus approvalStatus = SocialPostApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 20)
    @Builder.Default
    private SocialPostPublishStatus publishStatus = SocialPostPublishStatus.NOT_STARTED;

    @Lob
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "facebook_page_id", length = 100)
    private String facebookPageId;

    @Column(name = "facebook_post_id", length = 100)
    private String facebookPostId;

    @Column(name = "facebook_post_url", length = 500)
    private String facebookPostUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * False when the last failure was permanent (invalid token, permission
     * missing, vehicle deleted, invalid image) per the retry policy -
     * "Retry Failed" in the admin dashboard skips these.
     */
    @Column(name = "retryable", nullable = false)
    @Builder.Default
    private Boolean retryable = true;

    @Lob
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "approved_by_admin_id")
    private Long approvedByAdminId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
