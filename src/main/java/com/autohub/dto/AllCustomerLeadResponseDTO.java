package com.autohub.dto;

import com.autohub.entity.Vehicle;
import com.autohub.enums.CustomerLeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllCustomerLeadResponseDTO {


    private Long id;

    private String uniqueLeadId;

    private String customerName;

    private String customerMobile;

    private String customerCity;

    private String dealerName;

    private LocalDateTime enquiryDate;

    private CustomerLeadStatus leadStatus;

    private String vehicleName;
}
