package com.autohub.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDealerProfileRequestDTO {

    @NotBlank(message = "Business Name is Required")
    @Size(
            min = 3,
            max = 100,
            message = "Business Name must be between 3 and 100 characters"
    )
    private String businessName;
//
//    @NotBlank(message = "Owner Name is Required")
//    @Size(
//            min = 3,
//            max = 100,
//            message = "Owner Name must be between 3 and 100 characters"
//    )
//    @Pattern(
//            regexp = "^[A-Za-z ]+$",
//            message = "Owner Name must contain only alphabets and spaces"
//    )
//    private String ownerName;
//
//    @NotBlank(message = "Mobile Number is Required")
//    private String dealerMobile;

    @NotBlank(message = "WhatsAPP Number is Required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile Number must be a valid 10-digit Indian mobile number"
    )
    private String whatsapp;


    @Pattern(
            regexp = "^$|^[6-9]\\d{9}$",
            message = "Executive mobile number must be a valid 10-digit Indian mobile number"
    )
    private String executiveMobile;

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

    @NotBlank(message = "Pin code is Required")
    @NotBlank(message = "PinCode is Required")
    @Pattern(
            regexp = "^[1-9][0-9]{5}$",
            message = "Pincode must be a valid 6-digit PIN Code"
    )
    private String pinCode;

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


}
