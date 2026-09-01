package com.autohub.dto;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialVisitLogDTO {
    private Long id;
    private Long vehicleId;
    private String vehicleName;
    private Long dealerId;
    private String dealerName;
    private String source;
    private String postId;
    private String postUrl;
    private LocalDateTime visitedAt;
}
