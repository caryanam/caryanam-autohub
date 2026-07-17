package com.autohub.dto;

import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostPublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookDealerVehicleStatusDTO {

    private Long vehicleId;

    private String brand;

    private String model;

    private String variant;

    private Integer registrationYear;

    private BigDecimal askingPrice;

    private String primaryImageUrl;

    /** Null when the vehicle has never been requested for Facebook publishing. */
    private Long requestId;

    private SocialPostApprovalStatus approvalStatus;

    private SocialPostPublishStatus publishStatus;

    private String facebookPostUrl;

    /** Populated only when approvalStatus is REJECTED - the admin's reason. */
    private String rejectionReason;

    /**
     * Whether the dealer is currently allowed to select this vehicle and
     * click "Request Selected" - false if it's already PENDING, QUEUED,
     * PROCESSING, or PUBLISHED.
     */
    private boolean selectable;
}
