package com.autohub.dto;

import com.autohub.enums.SocialPostBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookBatchStatusDTO {

    private Long batchId;

    private SocialPostBatchStatus status;

    private int totalCount;

    private int successCount;

    private int failedCount;

    private int pendingCount;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private List<FacebookBatchItemStatusDTO> items;
}
