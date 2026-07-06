package com.autohub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegistrationRequestDTO {

    @NotBlank(message = "Customer Name is Required")
    @Size(min = 3, max = 100,message = "Customer Name must be between 3 and 100 characters")
    private String customerName;

    @NotBlank(message = "Customer Mobile Number is Required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Mobile Number must be a valid 10-digit Indian mobile number"
    )
    private String mobile;

    @NotBlank(message = "Customer City is Required")
    @Size(
            min = 3,
            max = 50,
            message = "City name must be between 3 and 50 characters"
    )
    @Pattern(
            regexp = "^[A-Za-z ]+$",
            message = "City name must contain only alphabets and spaces"
    )
    private String customerCity;

    @NotBlank(message = "Email is Required")
    @Email(message = "Invalid email format")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Please enter a valid email address"
    )
    @Size(max = 100, message = "Email must not exceed 100 characters")
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
