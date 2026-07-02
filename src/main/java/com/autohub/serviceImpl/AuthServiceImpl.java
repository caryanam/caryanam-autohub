package com.autohub.serviceImpl;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.LoginRequestDTO;
import com.autohub.dto.LoginResponseDTO;
import com.autohub.dto.ResetPasswordDTO;
import com.autohub.dto.VerifyOtpDTO;
import com.autohub.emailservice.EmailService;
import com.autohub.entity.Admin;
import com.autohub.entity.Customer;
import com.autohub.entity.Dealer;
import com.autohub.repository.AdminRepository;
import com.autohub.repository.CustomerRepository;
import com.autohub.repository.DealerRepository;
import com.autohub.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // TOKEN BLACKLIST
    private static final Set<String> tokenBlacklist = new HashSet<>();
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final DealerRepository dealerRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;

    // =====================================================
    // LOGIN
    // =====================================================

    // =====================================================
    // CHECK TOKEN
    // =====================================================
    public static boolean isTokenBlacklisted(String token) {

        return tokenBlacklist.contains(token);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {

        boolean isEmail = request.getUsername().contains("@");

        // =====================================
        // USER EXISTS CHECK
        // =====================================

        boolean exists;

        if (isEmail) {

            exists =
                    dealerRepository.existsByEmail(request.getUsername())
                            || adminRepository.existsByEmail(request.getUsername())
                            || customerRepository.existsByEmail(request.getUsername());

        } else {

            exists =
                    dealerRepository.existsByDealerMobile(request.getUsername())
                            || dealerRepository.existsByExecutiveMobile(request.getUsername())
                            || adminRepository.existsByMobile(request.getUsername())
                            || customerRepository.existsByMobile(request.getUsername());
        }

        if (!exists) {

            throw new RuntimeException(
                    "Account not found. Please register first."
            );
        }

        // =====================================
        // PASSWORD AUTHENTICATION
        // =====================================

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Invalid Password"
            );
        }

        // =====================================
        // DEALER LOGIN
        // =====================================


        Optional<Dealer> dealerOpt;

        if (isEmail) {

            dealerOpt =
                    dealerRepository.findByEmail(
                            request.getUsername()
                    );

        } else {

            dealerOpt =
                    dealerRepository.findByDealerMobile(
                            request.getUsername()
                    );

            if (dealerOpt.isEmpty()) {

                dealerOpt =
                        dealerRepository.findByExecutiveMobile(
                                request.getUsername()
                        );
            }
        }

        if (dealerOpt.isPresent()) {

            Dealer dealer = dealerOpt.get();

            String token = jwtUtil.generateToken(
                    dealer.getId(),
                    dealer.getOwnerName(),
                    dealer.getDealerMobile(),
                    dealer.getRole(),
                    dealer.getDealerMobile(),
                    dealer.getCity()
            );

            return new LoginResponseDTO(
                    dealer.getId(),
                    dealer.getRole().name(),
                    token
            );
        }

        // =====================================
        // ADMIN LOGIN
        // =====================================

        Optional<Admin> adminOpt =
                isEmail
                        ? adminRepository.findByEmail(request.getUsername())
                        : adminRepository.findByMobile(request.getUsername());

        if (adminOpt.isPresent()) {

            Admin admin = adminOpt.get();

            String token = jwtUtil.generateToken(
                    admin.getAdminId(),
                    admin.getFullName(),
                    admin.getMobile(),
                    admin.getRole(),
                    admin.getMobile(),
                    admin.getCity()
            );

            return new LoginResponseDTO(
                    admin.getAdminId(),
                    admin.getRole().name(),
                    token
            );
        }

        // =====================================
        // CUSTOMER LOGIN
        // =====================================

        Optional<Customer> customerOpt =
                isEmail
                        ? customerRepository.findByEmail(request.getUsername())
                        : customerRepository.findByMobile(request.getUsername());

        if (customerOpt.isPresent()) {

            Customer customer = customerOpt.get();

            String token = jwtUtil.generateToken(
                    customer.getId(),
                    customer.getCustomerName(),
                    customer.getMobile(),
                    customer.getRole(),
                    customer.getMobile(),
                    customer.getCustomerCity()
            );

            return new LoginResponseDTO(
                    customer.getId(),
                    customer.getRole().name(),
                    token
            );
        }

        throw new RuntimeException(
                "Login Failed"
        );
    }

    @Override
    public void logout(String token) {

        if (token == null || token.isBlank()) {

            throw new RuntimeException(
                    "Authorization Token is Missing"
            );
        }

        if (token.startsWith("Bearer ")) {

            token = token.substring(7);
        }

        tokenBlacklist.add(token);

        System.out.println("Logout Successfully");
    }


    //Forgot Password Common For ADMIN,DEALER,CUSTOMER

    @Override
    public String sendOtp(String email) {

        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000)
        );

        // Dealer
        Optional<Dealer> dealerOpt =
                dealerRepository.findByEmail(email);

        if (dealerOpt.isPresent()) {

            Dealer dealer = dealerOpt.get();

            dealer.setOtp(otp);
            dealer.setOtpGeneratedTime(LocalDateTime.now());
            dealer.setIsOtpVerified(false);

            dealerRepository.save(dealer);

            emailService.sendOtp(email, otp);

            return "OTP Sent Successfully";
        }

        // Customer
        Optional<Customer> customerOpt =
                customerRepository.findByEmail(email);

        if (customerOpt.isPresent()) {

            Customer customer = customerOpt.get();

            customer.setOtp(otp);
            customer.setOtpGeneratedTime(LocalDateTime.now());
            customer.setIsOtpVerified(false);

            customerRepository.save(customer);

            emailService.sendOtp(email, otp);

            return "OTP Sent Successfully";
        }

        // Admin
        Optional<Admin> adminOpt =
                adminRepository.findByEmail(email);

        if (adminOpt.isPresent()) {

            Admin admin = adminOpt.get();

            admin.setOtp(otp);
            admin.setOtpGeneratedTime(LocalDateTime.now());
            admin.setIsOtpVerified(false);

            adminRepository.save(admin);

            emailService.sendOtp(email, otp);

            return "OTP Sent Successfully";
        }

        throw new RuntimeException("User Not Found");
    }

    @Override
    public String verifyOtp(VerifyOtpDTO dto) {

        // Dealer
        Optional<Dealer> dealerOpt =
                dealerRepository.findByEmail(dto.getEmail());

        if (dealerOpt.isPresent()) {

            Dealer dealer = dealerOpt.get();

            validateOtp(
                    dealer.getOtp(),
                    dealer.getOtpGeneratedTime(),
                    dto.getOtp()
            );

            dealer.setIsOtpVerified(true);

            dealerRepository.save(dealer);

            return "OTP Verified Successfully";
        }

        // Customer
        Optional<Customer> customerOpt =
                customerRepository.findByEmail(dto.getEmail());

        if (customerOpt.isPresent()) {

            Customer customer = customerOpt.get();

            validateOtp(
                    customer.getOtp(),
                    customer.getOtpGeneratedTime(),
                    dto.getOtp()
            );

            customer.setIsOtpVerified(true);

            customerRepository.save(customer);

            return "OTP Verified Successfully";
        }

        // Admin
        Optional<Admin> adminOpt =
                adminRepository.findByEmail(dto.getEmail());

        if (adminOpt.isPresent()) {

            Admin admin = adminOpt.get();

            validateOtp(
                    admin.getOtp(),
                    admin.getOtpGeneratedTime(),
                    dto.getOtp()
            );

            admin.setIsOtpVerified(true);

            adminRepository.save(admin);

            return "OTP Verified Successfully";
        }

        throw new RuntimeException("User Not Found");
    }


    @Override
    public String resetPassword(ResetPasswordDTO dto) {

        // Dealer
        Optional<Dealer> dealerOpt =
                dealerRepository.findByEmail(dto.getEmail());

        if (dealerOpt.isPresent()) {

            Dealer dealer = dealerOpt.get();

            if (!Boolean.TRUE.equals(dealer.getIsOtpVerified())) {
                throw new RuntimeException("OTP Not Verified");
            }

            dealer.setPassword(
                    passwordEncoder.encode(dto.getNewPassword())
            );

            dealer.setOtp(null);
            dealer.setOtpGeneratedTime(null);
            dealer.setIsOtpVerified(false);

            dealerRepository.save(dealer);

            return "Password Reset Successfully";
        }

        // Customer
        Optional<Customer> customerOpt =
                customerRepository.findByEmail(dto.getEmail());

        if (customerOpt.isPresent()) {

            Customer customer = customerOpt.get();

            if (!Boolean.TRUE.equals(customer.getIsOtpVerified())) {
                throw new RuntimeException("OTP Not Verified");
            }

            customer.setPassword(
                    passwordEncoder.encode(dto.getNewPassword())
            );

            customer.setOtp(null);
            customer.setOtpGeneratedTime(null);
            customer.setIsOtpVerified(false);

            customerRepository.save(customer);

            return "Password Reset Successfully";
        }

        // Admin
        Optional<Admin> adminOpt =
                adminRepository.findByEmail(dto.getEmail());

        if (adminOpt.isPresent()) {

            Admin admin = adminOpt.get();

            if (!Boolean.TRUE.equals(admin.getIsOtpVerified())) {
                throw new RuntimeException("OTP Not Verified");
            }

            admin.setPassword(
                    passwordEncoder.encode(dto.getNewPassword())
            );

            admin.setOtp(null);
            admin.setOtpGeneratedTime(null);
            admin.setIsOtpVerified(false);

            adminRepository.save(admin);

            return "Password Reset Successfully";
        }

        throw new RuntimeException("User Not Found");
    }

    private void validateOtp(
            String savedOtp,
            LocalDateTime generatedTime,
            String enteredOtp) {

        if (savedOtp == null) {
            throw new RuntimeException("OTP Not Generated");
        }

        if (!savedOtp.equals(enteredOtp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (generatedTime.plusMinutes(5)
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException("OTP Expired");
        }
    }
}