package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.emailservice.EmailService;
import com.autohub.entity.Dealer;
import com.autohub.entity.Payment;
import com.autohub.enums.*;
import com.autohub.exception.ResourceNotFoundException;
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
    private final VehicleRepository vehicleRepository;
    private final CustomerLeadRepository leadRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final VehicleViewRepository vehicleViewRepository;
    private final PaymentRepository paymentRepository;

    @Value("${server.port}")
    private String baseUrl;



    @Override
    public DealerResponseDTO registerDealer(
            DealerRegisterDTO dto,
            MultipartFile dealerLogo,
            MultipartFile showroomImage) {

        if (dealerRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (dealerRepository.existsByDealerMobile(dto.getDealerMobile())) {
            throw new RuntimeException("Mobile already registered");
        }

        Dealer dealer = new Dealer();
        dealer.setBusinessName(dto.getBusinessName());
        dealer.setOwnerName(dto.getOwnerName());
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

        //Free trial for 1 month for dealer from registraion
        dealer.setFreeTrialEndDate(LocalDateTime.now().plusMonths(1));

        dealer.setRole(Role.DEALER);

        Dealer savedDealer = dealerRepository.save(dealer);

        if (dealerLogo != null && !dealerLogo.isEmpty()) {
            String logoPath = saveFile(
                    dealerLogo,
                    String.valueOf(savedDealer.getId()),
                    "logo");

            savedDealer.setDealerLogo(logoPath);
        }

        if (showroomImage != null && !showroomImage.isEmpty()) {
            String showroomPath = saveFile(
                    showroomImage,
                    String.valueOf(savedDealer.getId()),
                    "showroom");

            savedDealer.setShowroomImage(showroomPath);
        }

        savedDealer = dealerRepository.save(savedDealer);

        return modelMapper.map(savedDealer, DealerResponseDTO.class);
    }
//    @Override
//    public DealerResponseDTO registerDealer(DealerRegisterDTO dto, MultipartFile dealerLogo, MultipartFile showroomImage) {
//
//    if (dealerRepository.existsByEmail(dto.getEmail())) {
//        throw new RuntimeException("Email already registered");
//    }
//
//    if (dealerRepository.existsByMobile(dto.getMobile())) {
//        throw new RuntimeException("Mobile already registered");
//    }
//
//    validateImage(dealerLogo, "Dealer Logo");
//    validateImage(showroomImage, "Showroom Image");
//
//    Dealer dealer = new Dealer();
//    dealer.setBusinessName(dto.getBusinessName());
//    dealer.setOwnerName(dto.getOwnerName());
//    dealer.setGstNumber(dto.getGstNumber());
//    dealer.setYearsInBusiness(dto.getYearsInBusiness());
//    dealer.setMobile(dto.getMobile());
//    dealer.setWhatsapp(dto.getWhatsapp());
//    dealer.setEmail(dto.getEmail());
//    dealer.setPassword(passwordEncoder.encode(dto.getPassword()));
//    dealer.setAddress(dto.getAddress());
//    dealer.setDealerAccountStatus(DealerStatus.PENDING);
//    dealer.setCity(dto.getCity());
//    dealer.setState(dto.getState());
//    dealer.setPinCode(dto.getPinCode());
//    dealer.setRole(Role.DEALER);
//
//
//    Dealer savedDealer = dealerRepository.save(dealer);
//
//    String logoPath = saveFile(
//            dealerLogo,
//            String.valueOf(savedDealer.getId()),
//            "logo"
//    );
//
//    String showroomPath = saveFile(
//            showroomImage,
//            String.valueOf(savedDealer.getId()),
//            "showroom"
//    );
//
//    savedDealer.setDealerLogo(logoPath);
//    savedDealer.setShowroomImage(showroomPath);
//
//    savedDealer = dealerRepository.save(savedDealer);
//
//    return modelMapper.map(savedDealer, DealerResponseDTO.class);
//
//}

//    private void validateImage(MultipartFile file, String fieldName) {
//
//        if (file == null || file.isEmpty()) {
//            throw new RuntimeException(fieldName + " is required");
//        }
//
//        String fileName = file.getOriginalFilename();
//
//        if (fileName == null) {
//            throw new RuntimeException(fieldName + " is invalid");
//        }
//
//        String extension =
//                fileName.substring(fileName.lastIndexOf(".") + 1)
//                        .toLowerCase();
//
//        if (!extension.equals("jpg")
//                && !extension.equals("jpeg")
//                && !extension.equals("png")) {
//
//            throw new RuntimeException(
//                    fieldName + " must be JPG, JPEG or PNG format");
//        }
//    }


    private String saveFile(MultipartFile file,String dealerId,String prefix) {

        try {

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String extension =
                    file.getOriginalFilename()
                            .substring(
                                    file.getOriginalFilename()
                                            .lastIndexOf("."));

            String fileName =
                    dealerId +
                            "_" +
                            prefix +
                            extension;

            Path path =
                    Paths.get(uploadDir, fileName);

            Files.copy(
                    file.getInputStream(),
                    path,
                    StandardCopyOption.REPLACE_EXISTING);

            return path.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file");
        }
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
//
//        dto.setMonthlyViews(
//                vehicleViewService.getMonthlyViews(dealerId)
//                        .stream()
//                        .map(view -> view.getViews().intValue())
//                        .toList()
//        );
//
//        dto.setMonthlyLeads(
//                leadService.getMonthlyLead(dealerId)
//                        .stream()
//                        .map(lead -> lead.getLeads().intValue())
//                        .toList()
//        );

        return dto;
    }


    @Override
    public DealerResponseDTO getDealerProfile(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId).orElseThrow(() -> new ResourceNotFoundException("Dealer Not Found"));

        //return modelMapper.map(dealer,DealerResponseDTO.class);

       return DealerResponseDTO.builder()
                .id(dealer.getId())
                .businessName(dealer.getBusinessName())
                .ownerName(dealer.getOwnerName())
                .gstNumber(dealer.getGstNumber())
                .yearsInBusiness(dealer.getYearsInBusiness())
                .dealerMobile(dealer.getDealerMobile())
               .executiveMobile(dealer.getExecutiveMobile())
                .whatsapp(dealer.getWhatsapp())
                .email(dealer.getEmail())
                .address(dealer.getAddress())
                .city(dealer.getCity())
                .state(dealer.getState())
                .pinCode(dealer.getPinCode()).dealerLogo(
                       dealer.getDealerLogo() != null
                               ? "http://localhost:"+ baseUrl + "/" +
                               dealer.getDealerLogo().replace("\\", "/")
                               : null
               )

               .showroomImage(
                       dealer.getShowroomImage() != null
                               ? "http://localhost:"+ baseUrl + "/" +
                               dealer.getShowroomImage().replace("\\", "/")
                               : null
               )

               .dealerAccountStatus(dealer.getDealerAccountStatus())
                .createdAt(dealer.getCreatedAt())
                .build();

    }

    @Override
    public DealerProfileResponseDTO updateDealerProfile(Long id, UpdateDealerProfileRequestDTO dto) {

        Dealer dealer = dealerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dealer Not Found"));
        dealer.setBusinessName(dto.getBusinessName());
        dealer.setOwnerName(dto.getOwnerName());
        dealer.setWhatsapp(dto.getWhatsapp());
        dealer.setExecutiveMobile(dto.getExecutiveMobile());
        dealer.setAddress(dto.getAddress());
        dealer.setCity(dto.getCity());
        dealer.setPinCode(dto.getPinCode());
        dealer.setState(dto.getState());

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


        Payment payment = paymentRepository.findTopByDealerIdOrderByPaymentIdDesc(dealerId)
                .orElseThrow(() ->
                        new RuntimeException("You don't have any subscription plan."));

        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {

            throw new RuntimeException(
                    "Your subscription plan is waiting for admin approval.");
        }

        SubscriptionPlan plan = payment.getSubscriptionPlan();


        Long remainingDays = 0L;

        if (dealer.getSubscriptionEndDate() != null) {

            remainingDays = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    dealer.getSubscriptionEndDate().toLocalDate()
            );

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

}