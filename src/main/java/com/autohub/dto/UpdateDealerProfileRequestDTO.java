package com.autohub.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateDealerProfileRequestDTO {

    @NotBlank(message = "Business Name is Required")
    private String businessName;

    @NotBlank(message = "Owner Name is Required")
    private String ownerName;

    @NotBlank(message = "Mobile Number is Required")
    private String dealerMobile;

    @NotBlank(message = "WhatsAPP Number is Required")
    private String whatsapp;


    @Pattern(
            regexp = "^$|^[6-9]\\d{9}$",
            message = "Executive mobile number must be a valid 10-digit Indian mobile number"
    )
    private String executiveMobile;

    @NotBlank(message = "Address is Required")
    private String address;

    @NotBlank(message = "City is Required")
    private String city;

    @NotBlank(message = "Pin code is Required")
    private String pinCode;

    @NotBlank(message = "State is Required")
    private String state;


}
