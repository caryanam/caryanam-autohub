package com.autohub.entity;

import com.autohub.enums.DealerStatus;
import com.autohub.enums.Role;


import com.autohub.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Dealer_Reg")
@Data
public class Dealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerName;

    private String gstNumber;

    @Column(nullable = false)
    private int yearsInBusiness;

    @Column(nullable = false)
    private String dealerMobile;

    private String executiveMobile;

    @Column(nullable = false)
    private String whatsapp;

    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pinCode;

    @Column(length = 500)
    private String dealerLogo; //optional

    @Column(length = 500)
    private String showroomImage; //optional

    @Enumerated(EnumType.STRING)
    private Role role;

    private String otp;

    private LocalDateTime otpGeneratedTime;

    private Boolean isOtpVerified = false;

    @Enumerated(EnumType.STRING)
    private DealerStatus dealerAccountStatus; // PENDING, APPROVED

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Boolean subscriptionActive = false;

    @Enumerated(EnumType.STRING)
    private SubscriptionPlan subscriptionPlan;

    private LocalDateTime subscriptionStartDate;

    private LocalDateTime subscriptionEndDate;

    @Column(name = "free_trial_end_date")
    private LocalDateTime freeTrialEndDate;



}