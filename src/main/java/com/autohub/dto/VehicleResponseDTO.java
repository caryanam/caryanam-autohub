package com.autohub.dto;

import com.autohub.enums.InspectionStatus;
import com.autohub.enums.VehicleStatus;
import com.autohub.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponseDTO {

    private Long id;

    private Long dealerId;

    private String brand;

    private String model;

    private String variant;

    private Integer registrationYear;

    private BigDecimal askingPrice;

    private Long kilometerDriven;

    private String fuelType;

    private int ownershipDetails;

    private String city;

    private String vehicleDescription;

    private VehicleStatus vehicleStatus;

    private String dealerLogo;

    private String dealerShowroomImage;

    private VehicleType vehicleType;

    private LocalDateTime createdAt;

    private String dealerContactName;

    private String dealerContactNumber;

    private String executiveMobile;

    private String dealerContactEmail;

    private String dealerWhatsappNumber;

    private String dealerBusinessName;

    private int dealerYearsInBusiness;

    private String rtoInformation;

    private Boolean financeAvailability;

    private boolean isWishList;

    private Boolean certifyRequested;

    private Boolean isCertified;

    private InspectionStatus inspectionStatus;

    private LocalDateTime certifiedAt;

    private Long certifiedByInspectorId;

    private String certifiedByInspectorName;

    private String inspectionNotes;

    private List<String> images;

    private List<String> videos;
}
