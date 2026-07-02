package com.autohub.dto;

import com.autohub.enums.InsuranceStatus;
import com.autohub.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VehicleRequestDTO {

    @NotBlank(message = "Brand Name is Required")
    @Size(min = 5, max = 100,message = "Brand Name must be between 10 and 1000 characters")
    private String brand;

    @NotBlank(message = "Model Name is Required")
    @Size(min = 5, max = 100,message = "Model Name must be between 10 and 1000 characters")
    private String model;

    @NotBlank(message = "Variant Name is Required")
    @Size(min = 5, max = 100,message = "Variant Name must be between 10 and 1000 characters")
    private String variant;

    @NotNull(message = "Registration Year is Required")
    @Min(value = 1900, message = "Invalid Registration Year")
    private Integer registrationYear;

    @NotNull(message = "Asking Price is Required")
    @Positive(message = "Asking Price must be greater than 0")
    private Double askingPrice;

    @NotNull(message = "Kilometer Driven is Required")
    @Positive(message = "Kilometer Driven must be greater than 0")
    private Long kilometerDriven;

    @NotBlank(message = "Fuel Type is Required")
    @Pattern(
            regexp = "^(PETROL|DIESEL|CNG|LPG|ELECTRIC|HYBRID)$",
            message = "Invalid Fuel Type"
    )
    private String fuelType;

//    @NotBlank(message = "Transmission is Required")
//    @Pattern(
//            regexp = "^(MANUAL|AUTOMATIC|CVT|AMT)$",
//            message = "Invalid Transmission Type"
//    )
//    private String transmission;

    @NotNull(message = "Kilometer Driven is Required")
    @Positive(message = "Kilometer Driven must be greater than 0")
    private int ownershipDetails;

//    @NotBlank(message = "Insurance Status is Required")
//    private InsuranceStatus insuranceStatus;

    @NotBlank(message = "City is Required")
    @Size(min = 2, max = 50,
            message = "City must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "City can contain only letters and spaces"
    )
    private String city;

//    @NotBlank(message = "RTO Information is Required")
//    @Pattern(
//            regexp = "^[A-Z]{2}[0-9]{1,2}$",
//            message = "Invalid RTO Code. Example: MH31, DL1"
//    )
//    private String rtoInformation;

    @NotNull(message = "Finance Availability is Required")
    private Boolean financeAvailability;

    @NotBlank(message = "Vehicle Description is Required")
    @Size(min = 10, max = 1000,message = "Vehicle Description must be between 10 and 1000 characters")
    private String vehicleDescription;

    @NotNull(message = "Vehicle Type is Required")
    private VehicleType vehicleType;




}