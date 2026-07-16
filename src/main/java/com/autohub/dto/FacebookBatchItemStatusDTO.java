package com.autohub.dto;

import com.autohub.enums.SocialPostPublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookBatchItemStatusDTO {

    private Long requestId;

    private Long vehicleId;

    private String vehicleName;

    private SocialPostPublishStatus status;

    private int retryCount;

    private String errorMessage;

    private String facebookPostUrl;
}
