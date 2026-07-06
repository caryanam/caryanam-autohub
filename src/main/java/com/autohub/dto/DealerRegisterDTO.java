package com.autohub.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DealerRegisterDTO {

    @NotBlank(message = "Business Name is Required")
    @Size(
            min = 3,
            max = 100,
            message = "Business Name must be between 3 and 100 characters"
    )
    private String businessName;

    @NotBlank(message = "Owner Name is Required")
    @Size(
            min = 3,
            max = 100,
            message = "Owner Name must be between 3 and 100 characters"
    )
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "Owner Name must contain only alphabets and spaces"
    )
    private String ownerName;

    @Pattern(
            regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[A-Z0-9]{1}Z[A-Z0-9]{1}$",
            message = "GST Number must be a valid 15-digit Indian GST Number"
    )
    private String gstNumber;

    @NotNull(message = "Years In Business is Required")
    @Min(value = 0, message = "Years in business cannot be negative")
    @Max(value = 100, message = "Invalid years in business")
    private Integer yearsInBusiness;

    @NotBlank(message = "Dealer Mobile Number is Required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile Number must be a valid 10-digit Indian mobile number"
    )
    private String dealerMobile;


    @Pattern(
            regexp = "^$|^[6-9]\\d{9}$",
            message = "Executive mobile number must be a valid 10-digit Indian mobile number"
    )
    private String executiveMobile;

    @NotBlank(message = "Dealer WhatsApp Number is Required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile Number must be a valid 10-digit Indian mobile number"
    )
    private String whatsapp;

   @Email(message = "Invalid email format")
   private String email;

    @NotBlank(message = "Password is Required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number and 1 special character"
    )
    private String password;

    @NotBlank(message = "Address is Required")
    @Size(min = 5, max = 500,
            message = "Address must be between 5 and 500 characters")
    @Pattern(
            regexp = ".*[A-Za-z0-9].*",
            message = "Address must contain at least one letter or number"
    )
    private String address;

    @NotBlank(message = "City is Required")
    @Size(
            min = 3,
            max = 50,
            message = "City name must be between 3 and 50 characters"
    )
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "City name must contain only alphabets and spaces"
    )
    private String city;

    @NotBlank(message = "State is Required")
    @Size(
            min = 3,
            max = 50,
            message = "State name must be between 3 and 50 characters"
    )
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "State name must contain only alphabets and spaces"
    )
    private String state;

    @NotBlank(message = "PinCode is Required")
    @Pattern(
            regexp = "^[1-9][0-9]{5}$",
            message = "Pincode must be a valid 6-digit PIN Code"
    )
    private String pinCode;



}
