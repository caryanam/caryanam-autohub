package com.autohub.dto;

import com.autohub.enums.SubscriptionPlan;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DealerSubscriptionResponseDTO {

    private Long dealerId;

    private Long paymentId;

    private String dealerName;

    private LocalDateTime subscriptionStartDate;

    private LocalDateTime subscriptionEndDate;

    private Boolean subscriptionActive;

    private SubscriptionPlan subscriptionPlan;


}
