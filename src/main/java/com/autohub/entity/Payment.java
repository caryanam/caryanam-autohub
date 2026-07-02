package com.autohub.entity;

import com.autohub.enums.PaymentStatus;
import com.autohub.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    private SubscriptionPlan subscriptionPlan;

    private Double amount;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @CreationTimestamp
    private LocalDateTime paymentDate;

    @ManyToOne
    @JoinColumn(name = "dealer_id")
    private Dealer dealer;
}