package com.autohub.controller;

import com.autohub.dto.*;
import com.autohub.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;


    // ================= REGISTER CUSTOMER =================
    @PostMapping("/register")
    @Operation(summary = "Customer Registration API")
    public ResponseEntity<ResponseDto<CustomerRegistrationResponseDTO>> registerCustomer(@Valid @RequestBody CustomerRegistrationRequestDTO dto) {

        CustomerRegistrationResponseDTO responseDTO = customerService.customerRegistration(dto);

        return new ResponseEntity<>(new ResponseDto(200, "Customer Registration Successfully", responseDTO), HttpStatus.OK);
    }

    @PostMapping("/send-registration-otp")
    @Operation(summary = "Send Registration OTP API")
    public ResponseEntity<ResponseDto<String>> sendRegistrationOtp(@RequestParam String email) {
        String message = customerService.sendRegistrationOtp(email);
        return ResponseEntity.ok(
                new ResponseDto<>(200, "Registration OTP Sent", message)
        );
    }

    @PostMapping("/verify-registration-otp")
    @Operation(summary = "Verify Registration OTP API")
    public ResponseEntity<ResponseDto<String>> verifyRegistrationOtp(@RequestBody VerifyOtpDTO dto) {
        String message = customerService.verifyRegistrationOtp(dto.getEmail(), dto.getOtp());
        return ResponseEntity.ok(
                new ResponseDto<>(200, "Registration OTP Verified", message)
        );
    }

    //delete
    @DeleteMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(
            @RequestBody DeleteCustomerAccountRequestDTO request) {

        return ResponseEntity.ok(
                customerService.deleteCustomerAccount(request)
        );
    }

}