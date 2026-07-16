package com.autohub.dto;

import com.autohub.enums.SocialPostBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookBulkApprovePublishResponseDTO {

    private Long batchId;

    private SocialPostBatchStatus status;

    private int totalCount;
}
