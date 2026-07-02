package com.autohub.serviceImpl;

import com.autohub.dto.VehicleRequestDTO;
import com.autohub.dto.VehicleResponseDTO;
import com.autohub.dto.VehicleStatusRequestDTO;
import com.autohub.entity.*;
import com.autohub.enums.*;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

   private final VehicleRepository vehicleRepository;
   private final DealerRepository dealerRepository;
   private final VehicleMediaRepository mediaRepository;
   private final CustomerLeadRepository leadRepository;
   private final PaymentRepository paymentRepository;
   private final VehicleViewRepository vehicleViewRepository;
   private final WishlistRepository wishlistRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${server.port}")
    private String port;

    @Override
    public VehicleResponseDTO addVehicleWithData(VehicleRequestDTO vehicleRequestDTO, List<MultipartFile> images, List<MultipartFile> videos, Long dealerId) throws IOException {

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() ->
                        new RuntimeException("Dealer not found"));

        // Dealer account status Check
        if (dealer.getDealerAccountStatus() != DealerStatus.APPROVED) {
            throw new RuntimeException("Your account is pending for admin approval. You cannot add vehicles until approval.");
        }

        // ===============================
            // FREE TRIAL / SUBSCRIPTION CHECK
        // ===============================

        boolean freeTrialActive =
                dealer.getFreeTrialEndDate() != null
                        && LocalDateTime.now()
                        .isBefore(dealer.getFreeTrialEndDate());

        if (!freeTrialActive) {

            Payment payment = paymentRepository
                    .findTopByDealerIdOrderByPaymentIdDesc(dealerId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Your free trial has expired. Please purchase a subscription plan."));

            if (payment.getPaymentStatus() == PaymentStatus.PENDING) {

                throw new RuntimeException(
                        "Your subscription plan is waiting for admin approval.");
            }

            if (dealer.getSubscriptionEndDate() != null
                    && dealer.getSubscriptionEndDate()
                    .isBefore(LocalDateTime.now())) {

                throw new RuntimeException(
                        "Subscription expired. Please renew subscription.");
            }
        }

        if (!freeTrialActive && dealer.getSubscriptionPlan() == null) {
            throw new RuntimeException(
                    "Subscription plan not assigned. Please contact admin.");
        }


        // Vehicle Limit Check
        Long vehicleCount = vehicleRepository.countByDealer_Id(dealerId);

        if (freeTrialActive) {

            // Free Trial = BASIC Plan Limit
            int vehicleLimit = SubscriptionPlan.BASIC.getVehicleLimit();

            if (vehicleCount >= vehicleLimit) {

                throw new RuntimeException(
                        "Free trial allows only "
                                + vehicleLimit
                                + " vehicles.");
            }

        } else {

            if (dealer.getSubscriptionPlan() != null
                    && dealer.getSubscriptionPlan() != SubscriptionPlan.PREMIUM) {

                int vehicleLimit =
                        dealer.getSubscriptionPlan().getVehicleLimit();

                if (vehicleCount >= vehicleLimit) {

                    throw new RuntimeException(
                            "Vehicle limit exceeded. Your "
                                    + dealer.getSubscriptionPlan()
                                    + " plan allows only "
                                    + vehicleLimit
                                    + " vehicles.");
                }
            }
        }


        //Upload Image Validation Minimum 10 image required to add vehicle
        if (images == null || images.size() < 10) {
            throw new RuntimeException(
                    "Minimum 10 images are required");
        }

        //Upload Video Validation required to add vehicle
        if (videos == null || videos.isEmpty()) {
            throw new RuntimeException(
                    "Minimum 1 video is required");
        }

        for (MultipartFile image : images) {

            String contentType = image.getContentType();

            if (contentType == null ||
                    !(contentType.equalsIgnoreCase("image/jpeg")
                            || contentType.equalsIgnoreCase("image/jpg")
                            || contentType.equalsIgnoreCase("image/png"))) {

                throw new RuntimeException(
                        "Only JPG, JPEG and PNG images are allowed");
            }
        }

        // Save Vehicle
        Vehicle vehicle = Vehicle.builder()
                .dealer(dealer)
                .brand(vehicleRequestDTO.getBrand())
                .model(vehicleRequestDTO.getModel())
                .variant(vehicleRequestDTO.getVariant())
                .registrationYear(vehicleRequestDTO.getRegistrationYear())
                .fuelType(vehicleRequestDTO.getFuelType())
                //.transmission(vehicleRequestDTO.getTransmission())
                .kilometerDriven(vehicleRequestDTO.getKilometerDriven())
                .ownershipDetails(vehicleRequestDTO.getOwnershipDetails())
               // .insuranceStatus(InsuranceStatus.valueOf(String.valueOf(vehicleRequestDTO.getInsuranceStatus())))
                .askingPrice(vehicleRequestDTO.getAskingPrice())
                .vehicleDescription(vehicleRequestDTO.getVehicleDescription())
                .city(vehicleRequestDTO.getCity())
                .dealerContactName(dealer.getOwnerName())
                .dealerContactNumber(dealer.getDealerMobile())
                //.dealerWhatsappNumber(dealer.getWhatsapp())
                //.dealerBusinessName(dealer.getBusinessName())
               // .dealerContactEmail(dealer.getEmail())
               // .rtoInformation(vehicleRequestDTO.getRtoInformation())
                .financeAvailability(vehicleRequestDTO.getFinanceAvailability())
                .vehicleStatus(VehicleStatus.ACTIVE)
                .vehicleType(vehicleRequestDTO.getVehicleType())

                .createdAt(LocalDateTime.now())
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // IMAGE UPLOAD

        if (images != null && !images.isEmpty()) {

            long maxImageSize = 10 * 1024 * 1024; // 10 MB

            for (MultipartFile file : images) {

                String contentType = file.getContentType();

                // Image Type Validation
                if (contentType == null ||
                        !(contentType.equalsIgnoreCase("image/jpeg")
                                || contentType.equalsIgnoreCase("image/jpg")
                                || contentType.equalsIgnoreCase("image/png"))) {

                    throw new RuntimeException(
                            "Only JPG, JPEG and PNG images are allowed");
                }

                // Empty File Validation
                if (file.isEmpty()) {
                    throw new RuntimeException(
                            "Image file cannot be empty");
                }

                // File Size Validation
                if (file.getSize() > maxImageSize) {
                    throw new RuntimeException(
                            "Image size cannot exceed 10 MB");
                }

                String imageFolder =
                        uploadDir +
                                "/dealer_" + dealerId +
                                "/vehicle_" + savedVehicle.getId() +
                                "/images";

                File folder = new File(imageFolder);

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String fileName =
                        System.currentTimeMillis()
                                + "_"
                                + file.getOriginalFilename();

                Path filePath =
                        Paths.get(imageFolder, fileName);

                Files.copy(
                        file.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING);

                VehicleMedia media =
                        VehicleMedia.builder()
                                .fileName(fileName)
                                .fileType(file.getContentType())
                                .filePath(filePath.toString())
                                .mediaType("IMAGE")
                                .vehicle(savedVehicle)
                                .uploadedAt(LocalDateTime.now())
                                .build();

                mediaRepository.save(media);
            }
        }

        // VIDEO UPLOAD

        if (videos != null && !videos.isEmpty()) {

            long maxVideoSize = 10 * 1024 * 1024; // 10 MB

            for (MultipartFile file : videos) {

                String contentType = file.getContentType();

                // Video Type Validation
                if (contentType == null ||
                        !(contentType.equalsIgnoreCase("video/mp4")
                                || contentType.equalsIgnoreCase("video/quicktime")
                                || contentType.equalsIgnoreCase("video/x-msvideo"))) {

                    throw new RuntimeException(
                            "Minimum 1 video allowed with size under 10 mb Only MP4, MOV and AVI videos are allowed");
                }

                // Empty File Validation
                if (file.isEmpty()) {
                    throw new RuntimeException(
                            "Video file cannot be empty");
                }

                // File Size Validation
                if (file.getSize() > maxVideoSize) {
                    throw new RuntimeException(
                            "Video size cannot exceed 100 MB");
                }

                String videoFolder =
                        uploadDir +
                                "/dealer_" + dealerId +
                                "/vehicle_" + savedVehicle.getId() +
                                "/videos";

                File folder = new File(videoFolder);

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String fileName =
                        System.currentTimeMillis()
                                + "_"
                                + file.getOriginalFilename();

                Path filePath =
                        Paths.get(videoFolder, fileName);

                Files.copy(
                        file.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING);

                VehicleMedia media =
                        VehicleMedia.builder()
                                .fileName(fileName)
                                .fileType(file.getContentType())
                                .filePath(filePath.toString())
                                .mediaType("VIDEO")
                                .uploadedAt(LocalDateTime.now())
                                .vehicle(savedVehicle)
                                .build();

                mediaRepository.save(media);
            }
        }

        List<VehicleMedia> mediaList =
                mediaRepository.findByVehicleId(savedVehicle.getId());

        List<String> image = mediaList.stream()
                .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                .map(media -> "http://localhost:"+port + media.getFilePath().replace("\\", "/"))
                .toList();

        List<String> video = mediaList.stream()
                .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                .map(media -> "http://localhost:"+port + media.getFilePath().replace("\\", "/"))
                .toList();

        System.out.println("Media Count = " + mediaList.size());

        return VehicleResponseDTO.builder()
                .id(savedVehicle.getId())
                .dealerId(savedVehicle.getDealer().getId())
                .brand(savedVehicle.getBrand())
                .model(savedVehicle.getModel())
                .variant(savedVehicle.getVariant())
                .registrationYear(savedVehicle.getRegistrationYear())
                .fuelType(savedVehicle.getFuelType())
               // .transmission(savedVehicle.getTransmission())
                .kilometerDriven(savedVehicle.getKilometerDriven())
                .ownershipDetails(savedVehicle.getOwnershipDetails())
              //  .insuranceStatus(String.valueOf(savedVehicle.getInsuranceStatus()))
                .askingPrice(BigDecimal.valueOf(savedVehicle.getAskingPrice()))
                .vehicleDescription(savedVehicle.getVehicleDescription())
                .city(savedVehicle.getCity())
                .dealerContactName(vehicle.getDealer().getOwnerName())
                .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                .dealerBusinessName(vehicle.getDealer().getBusinessName())
                .dealerContactEmail(vehicle.getDealer().getEmail())
                .vehicleStatus(savedVehicle.getVehicleStatus())
                .vehicleType(savedVehicle.getVehicleType())
                .createdAt(savedVehicle.getCreatedAt())
               // .rtoInformation(savedVehicle.getRtoInformation())
                .financeAvailability(savedVehicle.isFinanceAvailability())
                .images(image)
                .videos(video)
                .build();

    }


    @Override
    public VehicleResponseDTO updateVehicle(Long id, VehicleRequestDTO request) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                        new ResourceNotFoundException("Vehicle not found with id: " + id));

        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setVariant(request.getVariant());
        vehicle.setRegistrationYear(request.getRegistrationYear());
        vehicle.setFuelType(request.getFuelType());
       // vehicle.setTransmission(request.getTransmission());
        vehicle.setKilometerDriven(request.getKilometerDriven());
        vehicle.setCity(request.getCity());
        vehicle.setOwnershipDetails(request.getOwnershipDetails());
       // vehicle.setInsuranceStatus(request.getInsuranceStatus());
        vehicle.setAskingPrice(request.getAskingPrice());
       // vehicle.setRtoInformation(request.getRtoInformation());
        vehicle.setFinanceAvailability(request.getFinanceAvailability());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setVehicleDescription(request.getVehicleDescription());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponseDTO.builder()
                .id(updatedVehicle.getId())
                .brand(updatedVehicle.getBrand())
                .model(updatedVehicle.getModel())
                .variant(updatedVehicle.getVariant())
                .city(updatedVehicle.getCity())
                .registrationYear(updatedVehicle.getRegistrationYear())
                .fuelType(updatedVehicle.getFuelType())
               // .transmission(updatedVehicle.getTransmission())
                .kilometerDriven(updatedVehicle.getKilometerDriven())
                .ownershipDetails(updatedVehicle.getOwnershipDetails())
               // .insuranceStatus(String.valueOf(updatedVehicle.getInsuranceStatus()))
                .askingPrice(BigDecimal.valueOf(updatedVehicle.getAskingPrice()))
                .vehicleType(updatedVehicle.getVehicleType())
                .vehicleDescription(updatedVehicle.getVehicleDescription())
                .build();
    }


    @Override
    public VehicleResponseDTO updateVehicleStatus(Long id,VehicleStatusRequestDTO request) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found"));

        if (!request.getStatus().equalsIgnoreCase("ACTIVE")
                && !request.getStatus().equalsIgnoreCase("INACTIVE") && !request.getStatus().equalsIgnoreCase("FEATURED")) {

            throw new RuntimeException("Status must be ACTIVE or INACTIVE");
        }

        vehicle.setVehicleStatus(VehicleStatus.valueOf(request.getStatus().toUpperCase()));

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponseDTO.builder()
                .id(updatedVehicle.getId())
                .vehicleStatus(VehicleStatus.valueOf(String.valueOf(updatedVehicle.getVehicleStatus())))
                .build();
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Vehicle not found with id: " + id));

        leadRepository.deleteLeadsByVehicleId(vehicle.getId());

        vehicleViewRepository.deleteByVehicleId(vehicle.getId());

        vehicleRepository.deleteById(vehicle.getId());



    }


    @Override
    public List<VehicleResponseDTO> getAllVehicleByDealerId(Long dealerId) {

        List<Vehicle> vehicles = vehicleRepository.findByDealerId(dealerId);

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("Dealer has no vehicles");
        }

        return vehicles.stream()
                .map(vehicle -> VehicleResponseDTO.builder()
                        .id(vehicle.getId())
                        .dealerId(vehicle.getDealer().getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .variant(vehicle.getVariant())
                        .registrationYear(vehicle.getRegistrationYear())
                        .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                        .kilometerDriven(vehicle.getKilometerDriven())
                        .fuelType(vehicle.getFuelType())
                    //    .transmission(vehicle.getTransmission())
                        .ownershipDetails(vehicle.getOwnershipDetails())
                      //  .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
                        .vehicleDescription(vehicle.getVehicleDescription())
                        .city(vehicle.getCity())
                        .dealerContactName(vehicle.getDealer().getOwnerName())
                        .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                        .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                        .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                        .dealerBusinessName(vehicle.getDealer().getBusinessName())
                        .dealerContactEmail(vehicle.getDealer().getEmail())
                        .vehicleStatus(vehicle.getVehicleStatus())
                        .vehicleType(vehicle.getVehicleType())
                    //    .rtoInformation(vehicle.getRtoInformation())
                        .financeAvailability(vehicle.isFinanceAvailability())
                        .createdAt(vehicle.getCreatedAt())
                        .images(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media -> "http://localhost:"+port+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )

                        .videos(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media ->"http://localhost:"+port+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )

                        .build())
                .toList();
    }

    @Override
    public VehicleResponseDTO getVehicleById(Long vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found with id : " + vehicleId));


        return VehicleResponseDTO.builder()
                .id(vehicle.getId())
                .dealerId(vehicle.getDealer().getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .variant(vehicle.getVariant())
                .registrationYear(vehicle.getRegistrationYear())
                .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                .kilometerDriven(vehicle.getKilometerDriven())
                .fuelType(vehicle.getFuelType())
           //     .transmission(vehicle.getTransmission())
                .ownershipDetails(vehicle.getOwnershipDetails())
           //     .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
                .vehicleDescription(vehicle.getVehicleDescription())
                .city(vehicle.getCity())
                .dealerContactName(vehicle.getDealer().getOwnerName())
                .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                .dealerBusinessName(vehicle.getDealer().getBusinessName())
                .dealerContactEmail(vehicle.getDealer().getEmail())
                .dealerYearsInBusiness(vehicle.getDealer().getYearsInBusiness())
                .vehicleStatus(vehicle.getVehicleStatus())
           //     .rtoInformation(vehicle.getRtoInformation())
                .financeAvailability(vehicle.isFinanceAvailability())
                .vehicleType(vehicle.getVehicleType())
                .createdAt(vehicle.getCreatedAt())
                .dealerLogo(
                        vehicle.getDealer().getDealerLogo() == null
                                ? null
                                : "http://localhost:" + port +
                                vehicle.getDealer().getDealerLogo().replace("\\", "/")
                )

                .dealerShowroomImage(
                        vehicle.getDealer().getShowroomImage() == null
                                ? null
                                : "http://localhost:" + port +
                                vehicle.getDealer().getShowroomImage().replace("\\", "/")
                )
                .images(
                        vehicle.getMediaList() == null
                                ? List.of()
                                : vehicle.getMediaList().stream()
                                .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                //.map(VehicleMedia::getFilePath)
                                .map(media -> "http://localhost:"+port+
                                        media.getFilePath().replace("\\", "/"))
                                .toList()
                )

                .videos(
                        vehicle.getMediaList() == null
                                ? List.of()
                                : vehicle.getMediaList().stream()
                                .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                                //.map(VehicleMedia::getFilePath)
                                .map(media -> "http://localhost:"+port+
                                        media.getFilePath().replace("\\", "/"))
                                .toList()
                )
                .build();
    }

    @Override
    public List<VehicleResponseDTO> getAllNonPremiumVehicle(Long customerId) {

        List<Vehicle> vehicles =
                vehicleRepository.findAllActiveAndFeaturedVehicles(
                        VehicleType.NON_PREMIUM);

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles found");
        }

        return vehicles.stream()
                .map(vehicle -> VehicleResponseDTO.builder()
                        .id(vehicle.getId())
                        .dealerId(vehicle.getDealer().getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .variant(vehicle.getVariant())
                        .registrationYear(vehicle.getRegistrationYear())
                        .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                        .kilometerDriven(vehicle.getKilometerDriven())
                        .fuelType(vehicle.getFuelType())
                        .ownershipDetails(vehicle.getOwnershipDetails())
                        .vehicleDescription(vehicle.getVehicleDescription())
                        .city(vehicle.getCity())
                        .dealerContactName(vehicle.getDealer().getOwnerName())
                        .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                        .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                        .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                        .dealerBusinessName(vehicle.getDealer().getBusinessName())
                        .dealerContactEmail(vehicle.getDealer().getEmail())
                        .vehicleStatus(vehicle.getVehicleStatus())
                        .vehicleType(vehicle.getVehicleType())
                        .createdAt(vehicle.getCreatedAt())
                        .financeAvailability(vehicle.isFinanceAvailability())
                        .images(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                        .map(media -> "http://localhost:" + port +
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )
                        .videos(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                                        .map(media -> "http://localhost:" + port +
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )
                        .isWishList(
                                wishlistRepository.existsByCustomer_IdAndVehicle_Id(
                                        customerId,
                                        vehicle.getId()
                                )
                        )
                        .build())
                .toList();
    }


    @Override
    public List<VehicleResponseDTO> getAllPremiumVehicle(Long customerId) {

        List<Vehicle> vehicles =
                vehicleRepository.findAllActiveAndFeaturedVehicles(
                        VehicleType.PREMIUM);

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles found");
        }

        return vehicles.stream()
                .map(vehicle -> VehicleResponseDTO.builder()
                        .id(vehicle.getId())
                        .dealerId(vehicle.getDealer().getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .variant(vehicle.getVariant())
                        .registrationYear(vehicle.getRegistrationYear())
                        .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                        .kilometerDriven(vehicle.getKilometerDriven())
                        .fuelType(vehicle.getFuelType())
                        .ownershipDetails(vehicle.getOwnershipDetails())
                        .vehicleDescription(vehicle.getVehicleDescription())
                        .city(vehicle.getCity())
                        .dealerContactName(vehicle.getDealer().getOwnerName())
                        .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                        .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                        .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                        .dealerBusinessName(vehicle.getDealer().getBusinessName())
                        .dealerContactEmail(vehicle.getDealer().getEmail())
                        .vehicleStatus(vehicle.getVehicleStatus())
                        .vehicleType(vehicle.getVehicleType())
                        .createdAt(vehicle.getCreatedAt())
                        .financeAvailability(vehicle.isFinanceAvailability())
                        .images(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                        .map(media -> "http://localhost:" + port +
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )
                        .videos(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                                        .map(media -> "http://localhost:" + port +
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )
                        .isWishList(
                                wishlistRepository.existsByCustomer_IdAndVehicle_Id(
                                        customerId,
                                        vehicle.getId()
                                )
                        )
                        .build())
                .toList();
    }


//
//    @Override
//    public List<VehicleResponseDTO> getAllNonPremiumVehicle(Long customerId) {
//
//        Pageable pageable = PageRequest.of(page, size);
//
//        List<Vehicle> vehicles =
//                vehicleRepository.findAllActiveAndFeaturedVehicles(
//                        VehicleType.NON_PREMIUM,
//                        pageable);
//
//        if (vehicles.isEmpty()) {
//            throw new ResourceNotFoundException("No vehicles found");
//        }
//
//        return vehicles.map(vehicle -> VehicleResponseDTO.builder()
//                .id(vehicle.getId())
//                .dealerId(vehicle.getDealer().getId())
//                .brand(vehicle.getBrand())
//                .model(vehicle.getModel())
//                .variant(vehicle.getVariant())
//                .registrationYear(vehicle.getRegistrationYear())
//                .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
//                .kilometerDriven(vehicle.getKilometerDriven())
//                .fuelType(vehicle.getFuelType())
//           //     .transmission(vehicle.getTransmission())
//                .ownershipDetails(vehicle.getOwnershipDetails())
//            //    .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
//                .vehicleDescription(vehicle.getVehicleDescription())
//                .city(vehicle.getCity())
//                .dealerContactName(vehicle.getDealer().getOwnerName())
//                .dealerContactNumber(vehicle.getDealer().getMobile())
//                .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
//                .dealerBusinessName(vehicle.getDealer().getBusinessName())
//                .dealerContactEmail(vehicle.getDealer().getEmail())
//                .vehicleStatus(vehicle.getVehicleStatus())
//                .vehicleType(vehicle.getVehicleType())
//                .createdAt(vehicle.getCreatedAt())
//            //    .rtoInformation(vehicle.getRtoInformation())
//                .financeAvailability(vehicle.isFinanceAvailability())
//                .images(
//                        vehicle.getMediaList() == null
//                                ? List.of()
//                                : vehicle.getMediaList().stream()
//                                .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
//                                .map(media -> "http://localhost:" + port +
//                                        media.getFilePath().replace("\\", "/"))
//                                .toList()
//                )
//
//                .videos(
//                        vehicle.getMediaList() == null
//                                ? List.of()
//                                : vehicle.getMediaList().stream()
//                                .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
//                                .map(media -> "http://localhost:" + port +
//                                        media.getFilePath().replace("\\", "/"))
//                                .toList()
//                )
//                .isWishList(
//                        wishlistRepository.existsByCustomer_IdAndVehicle_Id(
//                                customerId,
//                                vehicle.getId()
//                        )
//                )
//                .build());
//
//
//    }
//
//
//    @Override
//    public Page<VehicleResponseDTO> getAllPremiumVehicle(Long customerId,int page, int size) {
//
//        Pageable pageable = PageRequest.of(page, size);
//
//        Page<Vehicle> vehicles =
//                vehicleRepository.findAllActiveAndFeaturedVehicles(
//                        VehicleType.PREMIUM,
//                        pageable);
//
//        if (vehicles.isEmpty()) {
//            throw new ResourceNotFoundException("No vehicles found");
//        }
//
//        return vehicles.map(vehicle -> VehicleResponseDTO.builder()
//                .id(vehicle.getId())
//                .dealerId(vehicle.getDealer().getId())
//                .brand(vehicle.getBrand())
//                .model(vehicle.getModel())
//                .variant(vehicle.getVariant())
//                .registrationYear(vehicle.getRegistrationYear())
//                .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
//                .kilometerDriven(vehicle.getKilometerDriven())
//                .fuelType(vehicle.getFuelType())
//          //      .transmission(vehicle.getTransmission())
//                .ownershipDetails(vehicle.getOwnershipDetails())
//          //      .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
//                .vehicleDescription(vehicle.getVehicleDescription())
//                .city(vehicle.getCity())
//                .dealerContactName(vehicle.getDealer().getOwnerName())
//                .dealerContactNumber(vehicle.getDealer().getMobile())
//                .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
//                .dealerBusinessName(vehicle.getDealer().getBusinessName())
//                .dealerContactEmail(vehicle.getDealer().getEmail())
//                .vehicleStatus(vehicle.getVehicleStatus())
//                .vehicleType(vehicle.getVehicleType())
//                .createdAt(vehicle.getCreatedAt())
//         //       .rtoInformation(vehicle.getRtoInformation())
//                .financeAvailability(vehicle.isFinanceAvailability())
//
//                .images(
//                        vehicle.getMediaList() == null
//                                ? List.of()
//                                : vehicle.getMediaList().stream()
//                                .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
//                                .map(media -> "http://localhost:" + port +
//                                        media.getFilePath().replace("\\", "/"))
//                                .toList()
//                )
//
//                .videos(
//                        vehicle.getMediaList() == null
//                                ? List.of()
//                                : vehicle.getMediaList().stream()
//                                .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
//                                .map(media -> "http://localhost:" + port +
//                                        media.getFilePath().replace("\\", "/"))
//                                .toList()
//                )
//                .isWishList(
//                        wishlistRepository.existsByCustomer_IdAndVehicle_Id(
//                                customerId,
//                                vehicle.getId()
//                        )
//                )
//                .build());
//    }


    @Override
    public List<VehicleResponseDTO> getLatestFeaturedVehicles(Long customerId) {

        List<Vehicle> vehicles = vehicleRepository
                .findTop10ByVehicleStatusOrderByIdDesc(
                        VehicleStatus.FEATURED);

        return vehicles.stream()
                .map(vehicle -> VehicleResponseDTO.builder()
                        .id(vehicle.getId())
                        .dealerId(vehicle.getDealer().getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .variant(vehicle.getVariant())
                        .registrationYear(vehicle.getRegistrationYear())
                        .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                        .kilometerDriven(vehicle.getKilometerDriven())
                        .fuelType(vehicle.getFuelType())
                      //  .transmission(vehicle.getTransmission())
                        .ownershipDetails(vehicle.getOwnershipDetails())
                  //      .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
                        .vehicleDescription(vehicle.getVehicleDescription())
                        .city(vehicle.getCity())
                        .dealerContactName(vehicle.getDealer().getOwnerName())
                        .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                        .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                        .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                        .dealerBusinessName(vehicle.getDealer().getBusinessName())
                        .dealerContactEmail(vehicle.getDealer().getEmail())
                        .vehicleStatus(vehicle.getVehicleStatus())
                        .vehicleType(vehicle.getVehicleType())
                        .createdAt(vehicle.getCreatedAt())
                    //    .rtoInformation(vehicle.getRtoInformation())
                        .financeAvailability(vehicle.isFinanceAvailability())
                        .images(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media -> "http://localhost:"+port+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )

                        .videos(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))

                                        .map(media ->"http://localhost:"+port+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )
                        .isWishList(
                                wishlistRepository.existsByCustomer_IdAndVehicle_Id(
                                        customerId,
                                        vehicle.getId()
                                )
                        )

                        .build())
                .toList();
    }

    @Override
    public List<VehicleResponseDTO> getLatestVehicles(Long customerId) {


        List<Vehicle> vehicles =
                vehicleRepository.findTop10ByVehicleStatusInOrderByCreatedAtDesc(
                        List.of(VehicleStatus.ACTIVE,VehicleStatus.FEATURED)
                );

        return vehicles.stream()
                .filter(vehicle ->
                        vehicle.getVehicleStatus() == VehicleStatus.ACTIVE
                                || vehicle.getVehicleStatus() == VehicleStatus.FEATURED)
                .map(vehicle -> VehicleResponseDTO.builder()
                        .id(vehicle.getId())
                        .dealerId(vehicle.getDealer().getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .variant(vehicle.getVariant())
                        .registrationYear(vehicle.getRegistrationYear())
                        .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                        .kilometerDriven(vehicle.getKilometerDriven())
                        .fuelType(vehicle.getFuelType())
                    //    .transmission(vehicle.getTransmission())
                        .ownershipDetails(vehicle.getOwnershipDetails())
                   //     .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
                        .vehicleDescription(vehicle.getVehicleDescription())
                        .city(vehicle.getCity())
                        .dealerContactName(vehicle.getDealer().getOwnerName())
                        .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                        .executiveMobile(vehicle.getDealer().getExecutiveMobile())
                        .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                        .dealerBusinessName(vehicle.getDealer().getBusinessName())
                        .dealerContactEmail(vehicle.getDealer().getEmail())
                        .vehicleStatus(vehicle.getVehicleStatus())
                        .vehicleType(vehicle.getVehicleType())
                        .createdAt(vehicle.getCreatedAt())
                    //    .rtoInformation(vehicle.getRtoInformation())
                        .financeAvailability(vehicle.isFinanceAvailability())
                        .images(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media -> "http://localhost:"+port+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )

                        .videos(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media ->"http://localhost:"+port+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )
                        .isWishList(
                                wishlistRepository.existsByCustomer_IdAndVehicle_Id(
                                        customerId,
                                        vehicle.getId()
                                )
                        )

                        .build())
                .toList();
    }

}
