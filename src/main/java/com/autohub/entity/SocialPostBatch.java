package com.autohub.entity;

import com.autohub.enums.SocialPostBatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Created once per admin "Approve &amp; Publish" click. Scoped to a single
 * dealer and a set of (max FACEBOOK_BATCH_LIMIT) vehicle requests, which are
 * tracked individually via {@link SocialPostBatchItem}.
 */
@Entity
@Table(name = "social_post_batches", indexes = {
        @Index(name = "idx_spb_dealer_id", columnList = "dealer_id"),
        @Index(name = "idx_spb_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPostBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_spb_dealer"))
    private Dealer dealer;

    @Column(name = "approved_by_admin_id", nullable = false)
    private Long approvedByAdminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    @Builder.Default
    private SocialPostBatchStatus status = SocialPostBatchStatus.QUEUED;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "pending_count", nullable = false)
    private Integer pendingCount;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
