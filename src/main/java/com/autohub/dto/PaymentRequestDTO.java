package com.autohub.dto;

import com.autohub.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {

    @NotNull(message = "Dealer Id is Required")
    private Long dealerId;

    @NotNull(message = "Subscription Plan is Required : BASIC or STANDARD or PREMIUM")
    private SubscriptionPlan subscriptionPlan;
}
