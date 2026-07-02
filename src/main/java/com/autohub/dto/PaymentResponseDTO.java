package com.autohub.dto;

import com.autohub.entity.Dealer;
import com.autohub.enums.PaymentStatus;
import com.autohub.enums.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

    private Long paymentId;

    private Dealer dealer;

    private String businessName;

    private SubscriptionPlan subscriptionPlan;

    private Double amount;

    private String transactionId;

    private PaymentStatus paymentStatus;

    private LocalDateTime paymentDate;
}