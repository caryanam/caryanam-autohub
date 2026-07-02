package com.autohub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegistrationRequestDTO {

    @NotBlank(message = "Customer Name is Required")
    @Size(min = 3, max = 100,message = "Business Name must be between 3 and 100 characters")
    private String customerName;

    @NotBlank(message = "Customer Mobile Number is Required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile Number must be a valid 10-digit Indian mobile number"
    )
    private String mobile;

    @NotBlank(message = "Customer City is Required")
    @Size(min = 2, max = 50,
            message = "City must be between 2 and 50 characters")
    private String customerCity;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Customer Password is Required")
    @Size(min = 8, max = 20,
            message = "Password must be between 8 and 20 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,20}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number and 1 special character"
    )
    private String password;


}
