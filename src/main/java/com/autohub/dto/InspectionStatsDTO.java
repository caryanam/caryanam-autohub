package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionStatsDTO {
    private long totalRequests;
    private long pendingRequests;
    private long verifiedCount;
    private long rejectedCount;
    private long totalInspectors;
    private long activeInspectors;
}

