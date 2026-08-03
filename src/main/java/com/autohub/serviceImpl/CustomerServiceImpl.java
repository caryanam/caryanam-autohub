package com.autohub.serviceImpl;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.CustomerRegistrationRequestDTO;
import com.autohub.dto.CustomerRegistrationResponseDTO;
import com.autohub.dto.DeleteCustomerAccountRequestDTO;
import com.autohub.entity.Customer;
import com.autohub.enums.Role;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.CustomerLeadRepository;
import com.autohub.repository.CustomerRepository;
import com.autohub.repository.WishlistRepository;
import com.autohub.repository.EmailVerificationRepository;
import com.autohub.entity.EmailVerification;
import com.autohub.emailservice.EmailService;
import com.autohub.service.CustomerService;
import jakarta.transaction.Transactional;
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

    private final WishlistRepository wishlistRepository;

    private final CustomerLeadRepository customerLeadRepository;

    private final JwtUtil jwtUtil;

    private final EmailVerificationRepository emailVerificationRepository;

    private final EmailService emailService;


    @Override
    public CustomerRegistrationResponseDTO customerRegistration(CustomerRegistrationRequestDTO dto) {

        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (customerRepository.existsByMobile(dto.getMobile())) {
            throw new RuntimeException("Mobile already registered");
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            EmailVerification emailVerification = emailVerificationRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email not verified. Please verify your email first."));
            if (emailVerification.getIsVerified() == null || !emailVerification.getIsVerified()) {
                throw new RuntimeException("Email not verified. Please verify your email first.");
            }
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

    @Override
    @Transactional
    public String deleteCustomerAccount(DeleteCustomerAccountRequestDTO request) {

        Customer customer = customerRepository
                .findByEmail(request.getUsername())
                .or(() -> customerRepository.findByMobile(request.getUsername()))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid email/mobile"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Delete Wishlist
        wishlistRepository.deleteByCustomerId(customer.getId());

        // Delete Customer Leads
        customerLeadRepository.deleteByCustomerId(customer.getId());

        // Delete Customer
        customerRepository.delete(customer);

        return "Customer account deleted successfully.";
    }

    @Override
    public String sendRegistrationOtp(String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElse(new EmailVerification());

        if (verification.getOtpGeneratedTime() != null && verification.getOtpGeneratedTime().plusMinutes(1).isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Please wait 1 minute before requesting a new OTP");
        }

        verification.setEmail(email);
        verification.setOtp(otp);
        verification.setOtpGeneratedTime(LocalDateTime.now());
        verification.setIsVerified(false);

        emailVerificationRepository.save(verification);
        emailService.sendRegistrationOtp(email, otp);

        return "OTP sent successfully";
    }

    @Override
    public String verifyRegistrationOtp(String email, String otp) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not sent to this email"));

        if (verification.getIsVerified() != null && verification.getIsVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        if (verification.getOtp() == null || !verification.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (verification.getOtpGeneratedTime() == null ||
                verification.getOtpGeneratedTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        verification.setIsVerified(true);
        verification.setOtp("");
        verification.setOtpGeneratedTime(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        return "Email verified successfully";
    }
}
