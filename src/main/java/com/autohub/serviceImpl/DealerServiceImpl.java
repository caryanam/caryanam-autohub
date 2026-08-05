package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.emailservice.EmailService;
import com.autohub.entity.Dealer;
import com.autohub.entity.Payment;
import com.autohub.enums.*;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.entity.EmailVerification;
import com.autohub.repository.*;
import com.autohub.service.DealerService;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DealerServiceImpl implements DealerService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final DealerRepository dealerRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerLeadRepository leadRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final VehicleViewRepository vehicleViewRepository;
    private final PaymentRepository paymentRepository;

    @Value("${server.port}")
    private String baseUrl;

    @Value("${spring.server.url}")
    private String serverUrl;



    @Override
    public DealerResponseDTO registerDealer(
            DealerRegisterDTO dto,
            MultipartFile dealerLogo,
            MultipartFile showroomImage) {

        if (dealerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (dto.getExecutiveMobile() != null
                && !dto.getExecutiveMobile().isBlank()
                && dto.getDealerMobile().equals(dto.getExecutiveMobile())) {

            throw new RuntimeException(
                    "Dealer mobile number and Executive mobile number cannot be the same");
        }

        if (dealerRepository.existsByDealerMobile(dto.getDealerMobile())) {
            throw new RuntimeException("Mobile already registered");
        }

        if (dealerRepository.existsByWhatsapp(dto.getWhatsapp())) {
            throw new RuntimeException("WhatsApp number already registered");
        }

        Dealer dealer = new Dealer();
        dealer.setBusinessName(dto.getBusinessName());
        dealer.setOwnerName(dto.getOwnerName());
        dealer.setDateOfBirth(dto.getDateOfBirth());
        dealer.setGstNumber(dto.getGstNumber());
        dealer.setYearsInBusiness(dto.getYearsInBusiness());
        dealer.setDealerMobile(dto.getDealerMobile());
        dealer.setExecutiveMobile(dto.getExecutiveMobile());
        dealer.setWhatsapp(dto.getWhatsapp());
        dealer.setEmail(dto.getEmail());
        dealer.setPassword(passwordEncoder.encode(dto.getPassword()));
        dealer.setAddress(dto.getAddress());
        dealer.setDealerAccountStatus(DealerStatus.PENDING);
        dealer.setCity(dto.getCity());
        dealer.setState(dto.getState());
        dealer.setPinCode(dto.getPinCode());

        //Free trial for 1 month for dealer from registration date
        dealer.setFreeTrialEndDate(LocalDateTime.now().plusMonths(1));
        dealer.setSubscriptionPlan(SubscriptionPlan.BASIC);
        dealer.setSubscriptionStartDate(LocalDateTime.now());
        dealer.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        dealer.setSubscriptionActive(true);

        dealer.setRole(Role.DEALER);

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            EmailVerification emailVerification = emailVerificationRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email not verified. Please verify your email first."));
            if (!emailVerification.getIsVerified()) {
                throw new RuntimeException("Email not verified. Please verify your email first.");
            }
            dealer.setIsOtpVerified(true);
        } else {
            dealer.setIsOtpVerified(true);
        }

        Dealer savedDealer = dealerRepository.save(dealer);

        if (dealerLogo != null && !dealerLogo.isEmpty()) {

            if (!dealerLogo.getContentType().startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }

            if (dealerLogo.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("Logo size should be less than 5 MB");
            }

            String logoPath = saveFile(
                    dealerLogo,
                    String.valueOf(savedDealer.getId()),
                    "logo");

            savedDealer.setDealerLogo(logoPath);

        }

        if (showroomImage != null && !showroomImage.isEmpty()) {

            if (!showroomImage.getContentType().startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }

            if (showroomImage.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("Showroom Image size should be less than 5 MB");
            }
            String showroomPath = saveFile(
                    showroomImage,
                    String.valueOf(savedDealer.getId()),
                    "showroom");

            savedDealer.setShowroomImage(showroomPath);
        }

        savedDealer = dealerRepository.save(savedDealer);

        // OTP already verified during inline step

        return modelMapper.map(savedDealer, DealerResponseDTO.class);
    }

    // ============================================================
// Replace the existing saveFile method with this one.
// Add stripLeadingSlash as a new private helper alongside it.
// Nothing else in DealerServiceImpl needs to change.
// ============================================================

    private String saveFile(MultipartFile file, String dealerId, String prefix) {

        try {

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalName = file.getOriginalFilename();

            String extension =
                    (originalName != null && originalName.contains("."))
                            ? originalName.substring(originalName.lastIndexOf("."))
                            : "";

            String fileName = dealerId + "_" + prefix + extension;

            Path diskPath = Paths.get(uploadDir, fileName);

            Files.copy(
                    file.getInputStream(),
                    diskPath,
                    StandardCopyOption.REPLACE_EXISTING);

            // Build the PUBLIC (URL) path explicitly and independently of the
            // OS path representation used for the disk write above. Always
            // exactly one leading slash, always forward slashes - matches the
            // "/uploads/**" resource handler mapping and the pattern already
            // used in addVehicleWithData / the OLX import.
            //
            // Previously this returned path.toString() directly, which - once
            // uploadDir became a relative path - had NO leading slash, so
            // serverUrl + filePath produced a broken, unseparated URL like
            // "https://c1.caryanam.comuploads/dealers/127_logo.png".
            return "/" + stripLeadingSlash(uploadDir) + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file");
        }
    }

    /**
     * Removes any leading slash(es) from a configured directory value, so it
     * can be safely combined with exactly one "/" when building a public URL
     * path - regardless of whether file.upload-dir was set with or without
     * a leading slash.
     */
    private String stripLeadingSlash(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll("^/+", "");
    }


    @Override
    public DashboardResponseDTO getDashboard(Long dealerId) {

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Dealer not found"));

        DashboardResponseDTO dto = new DashboardResponseDTO();

        dto.setDealerName(dealer.getOwnerName());

        dto.setTotalVehicles( vehicleRepository.countByDealerId(dealer.getId()));

        dto.setFeaturedVehicles(vehicleRepository.countByDealer_IdAndVehicleStatus(dealer.getId(), VehicleStatus.FEATURED));

        dto.setTotalLeads(leadRepository.countByDealerId(dealer.getId()));

        dto.setVehicleViews(vehicleViewRepository.countViewsByDealerId(dealerId));
        return dto;
    }

    @Override
    public DealerResponseDTO getDealerProfile(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId).orElseThrow(() -> new ResourceNotFoundException("Dealer Not Found"));


        return DealerResponseDTO.builder()
                .id(dealer.getId())
                .businessName(dealer.getBusinessName())
                .ownerName(dealer.getOwnerName())
                .dateOfBirth(dealer.getDateOfBirth())
                .gstNumber(dealer.getGstNumber())
                .yearsInBusiness(dealer.getYearsInBusiness())
                .dealerMobile(dealer.getDealerMobile())
                .executiveMobile(dealer.getExecutiveMobile())
                .whatsapp(dealer.getWhatsapp())
                .email(dealer.getEmail())
                .address(dealer.getAddress())
                .city(dealer.getCity())
                .state(dealer.getState())
                .pinCode(dealer.getPinCode())
                .dealerLogo(buildMediaUrl(dealer.getDealerLogo()))
                .showroomImage(buildMediaUrl(dealer.getShowroomImage()))
                .dealerAccountStatus(dealer.getDealerAccountStatus())
                .createdAt(dealer.getCreatedAt())
                .build();

    }

    /**
     * Builds a full media URL from a stored path, normalizing it first so a
     * missing leading slash (e.g. from data saved before saveFile() was
     * fixed) can never again produce a broken concatenated URL like
     * "https://c1.caryanam.comuploads/dealers/127_logo.jpg".
     */
    private String buildMediaUrl(String storedPath) {

        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String normalized = storedPath.replace("\\", "/").trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return serverUrl + normalized;
    }


    @Override
    public DealerProfileResponseDTO updateDealerProfile(Long id, UpdateDealerProfileRequestDTO dto, MultipartFile dealerLogo, MultipartFile showroomImage) {

        Dealer dealer = dealerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dealer Not Found"));
        dealer.setBusinessName(dto.getBusinessName());
        dealer.setWhatsapp(dto.getWhatsapp());
        dealer.setExecutiveMobile(dto.getExecutiveMobile());
        dealer.setDateOfBirth(dto.getDateOfBirth());
        dealer.setAddress(dto.getAddress());
        dealer.setCity(dto.getCity());
        dealer.setPinCode(dto.getPinCode());
        dealer.setState(dto.getState());

        if (dealerLogo != null && !dealerLogo.isEmpty()) {
            if (!dealerLogo.getContentType().startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }
            if (dealerLogo.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("Logo size should be less than 5 MB");
            }
            String logoPath = saveFile(dealerLogo, String.valueOf(dealer.getId()), "logo");
            dealer.setDealerLogo(logoPath);
        }

        if (showroomImage != null && !showroomImage.isEmpty()) {
            if (!showroomImage.getContentType().startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }
            if (showroomImage.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("Showroom Image size should be less than 5 MB");
            }
            String showroomPath = saveFile(showroomImage, String.valueOf(dealer.getId()), "showroom");
            dealer.setShowroomImage(showroomPath);
        }

        Dealer save = dealerRepository.save(dealer);

        return modelMapper.map(save,DealerProfileResponseDTO.class);

    }

    @Override
    public DealerResponseDTO updateDealerAccountStatus(Long dealerId,DealerAccountStatusRequestDTO requestDTO) {

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Dealer not found"));

        if (requestDTO.getStatus() == null ||
                requestDTO.getStatus().trim().isEmpty()) {

            throw new RuntimeException("Status is required");
        }

        DealerStatus newStatus;

        try {
            newStatus = DealerStatus.valueOf(
                    requestDTO.getStatus().trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid status. Only PENDING and APPROVED are allowed");
        }

        DealerStatus currentStatus = dealer.getDealerAccountStatus();

        if (currentStatus == null) {
            currentStatus = DealerStatus.PENDING;
        }

        if (currentStatus.equals(newStatus)) {
            throw new RuntimeException(
                    "Dealer account already " + currentStatus);
        }

        dealer.setDealerAccountStatus(newStatus);

        Dealer updatedDealer = dealerRepository.save(dealer);

        return modelMapper.map(updatedDealer, DealerResponseDTO.class);
    }

    @Override
    public List<DealerSubscriptionResponseDTO> getSubscriptions() {

        return dealerRepository.findAll()
                .stream()
                .map(dealer -> {

                    DealerSubscriptionResponseDTO dto = new DealerSubscriptionResponseDTO();

                    dto.setDealerId(dealer.getId());
                    dto.setDealerName(dealer.getBusinessName());
                    dto.setSubscriptionStartDate(dealer.getSubscriptionStartDate());
                    Optional<Payment> payment =
                            paymentRepository.findTopByDealerIdOrderByPaymentIdDesc(dealer.getId());

                    dto.setPaymentId(
                            payment.map(Payment::getPaymentId).orElse(null)
                    );

                    dto.setSubscriptionEndDate(dealer.getSubscriptionEndDate());

                    dto.setSubscriptionActive( dealer.getSubscriptionActive());

                    dto.setSubscriptionPlan(   dealer.getSubscriptionPlan());

                    return dto;

                })
                .toList();
    }

    @Override
    public List<SubscriptionPlanDTO> getAllSubscriptionsPlans() {

        return Arrays.stream(SubscriptionPlan.values())
                .map(plan -> new SubscriptionPlanDTO(
                        plan.name(),
                        plan.getAmount(),
                        plan.getVehicleLimit(),
                        plan.getValidityMonths()
                ))
                .toList();
    }


    @Override
    public DealerCurrentSubscriptionPlanDTO getDealerCurrentSubscriptionPlan(Long dealerId) {

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Dealer Not Found"));

        Optional<Payment> paymentOpt =
                paymentRepository.findTopByDealerIdOrderByPaymentIdDesc(dealerId);

        SubscriptionPlan plan;

        if (paymentOpt.isPresent()
                && paymentOpt.get().getPaymentStatus() == PaymentStatus.SUCCESS) {

            plan = paymentOpt.get().getSubscriptionPlan();

        } else {

            plan = dealer.getSubscriptionPlan();
        }

        Long remainingDays = 0L;

        if (dealer.getSubscriptionEndDate() != null) {

            remainingDays = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    dealer.getSubscriptionEndDate().toLocalDate());

            if (remainingDays < 0) {
                remainingDays = 0L;
            }
        }

        return new DealerCurrentSubscriptionPlanDTO(
                dealer.getId(),
                plan.name(),
                plan.getAmount(),
                plan.getVehicleLimit(),
                plan.getValidityMonths(),
                dealer.getSubscriptionStartDate(),
                dealer.getSubscriptionEndDate(),
                remainingDays
        );
    }

    @Override
    public String sendRegistrationOtp(String email) {
        if (dealerRepository.existsByEmail(email)) {
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
        emailService.sendRegistrationOtp(email, otp, "Dealer");

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