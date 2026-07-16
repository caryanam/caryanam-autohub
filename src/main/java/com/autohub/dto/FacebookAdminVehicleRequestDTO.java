package com.autohub.dto;

import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostPublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookAdminVehicleRequestDTO {

    private Long requestId;

    private Long vehicleId;

    private String brand;

    private String model;

    private String variant;

    private String primaryImageUrl;

    private BigDecimal askingPrice;

    private String fuelType;

    private Integer registrationYear;

    private SocialPostApprovalStatus approvalStatus;

    private SocialPostPublishStatus publishStatus;

    private LocalDateTime requestedAt;
}
