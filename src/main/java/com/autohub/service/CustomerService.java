package com.autohub.service;

import com.autohub.dto.CustomerRegistrationRequestDTO;
import com.autohub.dto.CustomerRegistrationResponseDTO;
import org.springframework.stereotype.Service;

@Service
public interface CustomerService {

    CustomerRegistrationResponseDTO customerRegistration(CustomerRegistrationRequestDTO requestDTO);
}
