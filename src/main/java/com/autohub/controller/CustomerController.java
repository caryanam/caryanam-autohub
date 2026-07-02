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

}