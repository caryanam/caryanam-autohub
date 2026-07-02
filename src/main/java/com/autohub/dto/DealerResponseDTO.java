package com.autohub.dto;

import com.autohub.enums.DealerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealerResponseDTO {

    private Long id;

    private String businessName;
    private String ownerName;
    private String gstNumber;
    private Integer yearsInBusiness;

    private String dealerMobile;
    private String executiveMobile;
    private String whatsapp;
    private String email;
    private String password;

    private String address;
    private String city;
    private String state;
    private String pinCode;

    private String dealerLogo;
    private String showroomImage;

    private DealerStatus dealerAccountStatus;

    private LocalDateTime createdAt;

}
