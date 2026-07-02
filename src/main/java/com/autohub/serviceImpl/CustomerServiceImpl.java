package com.autohub.serviceImpl;

import com.autohub.dto.CustomerRegistrationRequestDTO;
import com.autohub.dto.CustomerRegistrationResponseDTO;
import com.autohub.entity.Customer;
import com.autohub.enums.Role;
import com.autohub.repository.CustomerRepository;
import com.autohub.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    private final PasswordEncoder passwordEncoder;

    private final ModelMapper modelMapper;


    @Override
    public CustomerRegistrationResponseDTO customerRegistration(CustomerRegistrationRequestDTO dto) {

        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (customerRepository.existsByMobile(dto.getMobile())) {
            throw new RuntimeException("Mobile already registered");
        }


        Customer customer = new Customer();
        customer.setCustomerName(dto.getCustomerName());
        customer.setMobile(dto.getMobile());
        customer.setCustomerCity(dto.getCustomerCity());
        customer.setEmail(dto.getEmail());
        customer.setPassword(passwordEncoder.encode(dto.getPassword()));
        customer.setAccountCreatedAt(LocalDateTime.now());
        customer.setRole(Role.CUSTOMER);

        Customer save = customerRepository.save(customer);



        return modelMapper.map(save, CustomerRegistrationResponseDTO.class);
    }
}
