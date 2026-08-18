package com.autohub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dealer_whatsapp_otp_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerWhatsappOtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String whatsapp;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime otpGeneratedTime;

    @Column(nullable = false)
    private Boolean isVerified = false;

}
