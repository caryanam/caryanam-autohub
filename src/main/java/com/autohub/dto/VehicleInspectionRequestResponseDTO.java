package com.autohub.dto;

import com.autohub.enums.InspectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInspectionRequestResponseDTO {

    private Long id;

    // Vehicle Info
    private Long vehicleId;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleVariant;
    private Integer registrationYear;
    private BigDecimal askingPrice;
    private Long kilometerDriven;
    private String fuelType;
    private String vehicleCity;
    private String vehicleType;
    private String vehicleThumbnail;
    private List<String> vehicleImages;

    // Dealer Info
    private Long dealerId;
    private String dealerBusinessName;
    private String dealerOwnerName;
    private String dealerMobile;
    private String dealerWhatsapp;
    private String dealerEmail;
    private String dealerAddress;
    private String dealerCity;

    // Inspector Info
    private Long inspectorId;
    private String inspectorName;
    private String inspectorMobile;
    private String inspectorEmail;
    private String inspectorAssignedArea;

    // Request & Status Info
    private String area;
    private InspectionStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime completedDate;
    private String inspectionNotes;
    private Integer overallRating;
    private Boolean isCertified;
}

