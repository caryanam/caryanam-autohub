package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookAdminDealerSummaryDTO {

    private Long dealerId;

    private String dealerBusinessName;

    private long pendingCount;

    private long processingCount;

    private long publishedCount;

    private long failedCount;
}
