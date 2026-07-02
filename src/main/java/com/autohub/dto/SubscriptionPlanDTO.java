package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionPlanDTO {

    private String planName;
    private Double amount;
    private Integer vehicleLimit;
    private Integer validityMonths;
}
