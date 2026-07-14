package com.autohub.service;

import com.autohub.dto.CustomerRegistrationRequestDTO;
import com.autohub.dto.CustomerRegistrationResponseDTO;
import com.autohub.dto.DeleteCustomerAccountRequestDTO;
import org.springframework.stereotype.Service;

@Service
public interface CustomerService {

    CustomerRegistrationResponseDTO customerRegistration(CustomerRegistrationRequestDTO requestDTO);

    String deleteCustomerAccount(DeleteCustomerAccountRequestDTO request);
}
