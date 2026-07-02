package com.autohub.dto;

import com.autohub.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DealerCurrentSubscriptionPlanDTO {

    private Long dealerId;
    private String subscriptionPlan;
    private Double amount;
    private Integer vehicleLimit;
    private Integer validityMonths;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private Long remainingDays;
}
