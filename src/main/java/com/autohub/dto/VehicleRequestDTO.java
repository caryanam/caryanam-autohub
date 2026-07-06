package com.autohub.dto;

import com.autohub.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VehicleRequestDTO {

    @NotBlank(message = "Brand Name is Required")
    @Size(min = 2, max = 100,message = "Brand Name must be between 10 and 1000 characters")
    private String brand;

    @NotBlank(message = "Model Name is Required")
    @Size(min = 2, max = 100,message = "Model Name must be between 10 and 1000 characters")
    private String model;

    @NotBlank(message = "Variant Name is Required")
    @Size(min = 2, max = 100,message = "Variant Name must be between 10 and 1000 characters")
    private String variant;

    @NotNull(message = "Registration Year is Required")
    @Min(value = 1900, message = "Invalid Registration Year")
    private Integer registrationYear;

    @NotNull(message = "Asking Price is Required")
    @Positive(message = "Asking Price must be greater than 0")
    @DecimalMin(
            value = "10000",
            message = "Asking Price must be at least ₹10,000"
    )
    private Double askingPrice;

    @NotNull(message = "Kilometer Driven is Required")
    @PositiveOrZero(message = "Kilometer Driven cannot be negative")
    private Long kilometerDriven;

    @NotBlank(message = "Fuel Type is Required")
    @Pattern(
            regexp = "^(PETROL|DIESEL|CNG|LPG|ELECTRIC|HYBRID)$",
            message = "Invalid Fuel Type"
    )
    private String fuelType;

    @NotNull(message = "Kilometer Driven is Required")
    @Positive(message = "Kilometer Driven must be greater than 0")
    private int ownershipDetails;


    @NotBlank(message = "City is Required")
    @Size(min = 2, max = 50,
            message = "City must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "City can contain only letters and spaces"
    )
    private String city;

    @NotNull(message = "Finance Availability is Required")
    private Boolean financeAvailability;

    @NotBlank(message = "Vehicle Description is Required")
    @Size(
            min = 20,
            max = 5000,
            message = "Vehicle Description must be between 20 and 5000 characters"
    )
    @Pattern(
            regexp = ".*[A-Za-z0-9].*",
            message = "Vehicle Description must contain meaningful text"
    )
    private String vehicleDescription;

    @NotNull(message = "Vehicle Type is Required")
    private VehicleType vehicleType;




}