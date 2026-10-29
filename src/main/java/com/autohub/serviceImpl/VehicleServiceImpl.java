package com.autohub.serviceImpl;

import com.autohub.dto.VehicleRequestDTO;
import com.autohub.dto.VehicleResponseDTO;
import com.autohub.dto.VehicleStatusRequestDTO;
import com.autohub.entity.*;
import com.autohub.enums.*;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.InspectorService;
import com.autohub.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final InstagramPostBatchItemRepository instagramPostBatchItemRepository;
    private final SocialPostBatchItemRepository socialPostBatchItemRepository;
    private final VehicleInstagramPostRequestRepository vehicleInstagramPostRequestRepository;
    private final VehicleSocialPostRequestRepository vehicleSocialPostRequestRepository;
    private final SocialVisitLogRepository socialVisitLogRepository;
    private final VehicleInspectionRequestRepository inspectionRequestRepository;
    private final InspectorService inspectorService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${server.port}")
    private String port;

    @Value("${spring.server.url}")
    private String serverUrl;

    @Override
    @Transactional
    public VehicleResponseDTO addVehicleWithData(
            VehicleRequestDTO vehicleRequestDTO,
            List<MultipartFile> images,
            List<MultipartFile> videos,
            Long dealerId
    ) throws IOException {

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new RuntimeException("Dealer not found"));

        // Dealer account status Check
        if (dealer.getDealerAccountStatus() != DealerStatus.APPROVED) {
            throw new RuntimeException("Your account is pending for admin approval. You cannot add vehicles until approval.");
        }

        // ===============================
        // FREE TRIAL / SUBSCRIPTION CHECK
        // ===============================
        boolean freeTrialActive = dealer.getFreeTrialEndDate() != null
                && LocalDateTime.now().isBefore(dealer.getFreeTrialEndDate());

        if (!freeTrialActive) {
            Payment payment = paymentRepository
                    .findTopByDealerIdOrderByPaymentIdDesc(dealerId)
                    .orElseThrow(() -> new RuntimeException(
                            "Your free plan is expired. Please upgrade the subscription plan !!"));

            if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                throw new RuntimeException("Your subscription plan is waiting for admin approval.");
            }

            if (dealer.getSubscriptionEndDate() != null
                    && dealer.getSubscriptionEndDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Subscription expired. Please renew subscription.");
            }
        }

        if (!freeTrialActive && dealer.getSubscriptionPlan() == null) {
            throw new RuntimeException("Subscription plan not assigned. Please contact admin.");
        }

        // Vehicle Limit Check
        Long vehicleCount = vehicleRepository.countByDealer_Id(dealerId);
        if (!freeTrialActive) {
            if (dealer.getSubscriptionPlan() != null
                    && dealer.getSubscriptionPlan() != SubscriptionPlan.PREMIUM) {
                int vehicleLimit = dealer.getSubscriptionPlan().getVehicleLimit();
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

        // Duplicate Vehicle Check
        List<Vehicle> dealerVehicles = vehicleRepository.findByDealerId(dealerId);
        boolean isDuplicate = dealerVehicles.stream().anyMatch(v ->
                v.getBrand() != null && v.getBrand().equalsIgnoreCase(vehicleRequestDTO.getBrand()) &&
                v.getModel() != null && v.getModel().equalsIgnoreCase(vehicleRequestDTO.getModel()) &&
                v.getVariant() != null && v.getVariant().equalsIgnoreCase(vehicleRequestDTO.getVariant()) &&
                v.getCity() != null && v.getCity().equalsIgnoreCase(vehicleRequestDTO.getCity()) &&
                v.getFuelType() != null && v.getFuelType().equalsIgnoreCase(vehicleRequestDTO.getFuelType() != null ? vehicleRequestDTO.getFuelType().trim() : null) &&
                v.getRegistrationYear() != null && v.getRegistrationYear().equals(vehicleRequestDTO.getRegistrationYear())
        );

        if (isDuplicate) {
            throw new RuntimeException("Duplicate vehicle: This vehicle already exists in your inventory.");
        }

        // Upload Image Validation Minimum 5 images required
        if (images == null || images.size() < 5) {
            throw new RuntimeException("Minimum 5 images are required");
        }

        for (MultipartFile image : images) {
            String contentType = image.getContentType();
            if (contentType == null ||
                    !(contentType.equalsIgnoreCase("image/jpeg")
                            || contentType.equalsIgnoreCase("image/jpg")
                            || contentType.equalsIgnoreCase("image/png"))) {
                throw new RuntimeException("Only JPG, JPEG and PNG images are allowed");
            }
        }

        boolean certifyReq = Boolean.TRUE.equals(vehicleRequestDTO.getCertifyRequested());

        // Save Vehicle
        Vehicle vehicle = Vehicle.builder()
                .dealer(dealer)
                .brand(vehicleRequestDTO.getBrand())
                .model(vehicleRequestDTO.getModel())
                .variant(vehicleRequestDTO.getVariant() != null ? vehicleRequestDTO.getVariant() : "")
                .registrationYear(vehicleRequestDTO.getRegistrationYear())
                .fuelType(vehicleRequestDTO.getFuelType().trim().toUpperCase())
                .kilometerDriven(vehicleRequestDTO.getKilometerDriven())
                .ownershipDetails(vehicleRequestDTO.getOwnershipDetails())
                .askingPrice(vehicleRequestDTO.getAskingPrice())
                .vehicleDescription(vehicleRequestDTO.getVehicleDescription().trim().replaceAll("\\s+", " "))
                .city(vehicleRequestDTO.getCity())
                .dealerContactName(dealer.getOwnerName())
                .dealerContactNumber(dealer.getDealerMobile())
                .financeAvailability(vehicleRequestDTO.getFinanceAvailability())
                .vehicleStatus(VehicleStatus.ACTIVE)
                .vehicleType(vehicleRequestDTO.getVehicleType())
                .certifyRequested(certifyReq)
                .isCertified(false)
                .inspectionStatus(certifyReq ? InspectionStatus.PENDING : InspectionStatus.NONE)
                .createdAt(LocalDateTime.now())
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // If dealer selected Yes for certification, log inspection request in dealer's area
        if (certifyReq) {
            inspectorService.createOrUpdateInspectionRequestForVehicle(savedVehicle.getId(), dealerId);
        }

        // IMAGE UPLOAD
        if (images != null && !images.isEmpty()) {
            long maxImageSize = 10 * 1024 * 1024; // 10 MB

            for (MultipartFile file : images) {
                if (file.isEmpty()) {
                    throw new RuntimeException("Image file cannot be empty");
                }
                if (file.getSize() > maxImageSize) {
                    throw new RuntimeException("Image size cannot exceed 10 MB");
                }

                String imageFolder = uploadDir + "/dealer_" + dealerId + "/vehicle_" + savedVehicle.getId() + "/images";
                File folder = new File(imageFolder);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String safeOriginalName = sanitizeFileName(file.getOriginalFilename());
                String fileName = System.currentTimeMillis() + "_" + safeOriginalName;
                Path targetLocation = Paths.get(imageFolder).resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                String publicFilePath = "/" + stripLeadingSlash(uploadDir) + "/dealer_" + dealerId + "/vehicle_" + savedVehicle.getId() + "/images/" + fileName;

                VehicleMedia media = VehicleMedia.builder()
                        .fileName(fileName)
                        .fileType(file.getContentType())
                        .filePath(publicFilePath)
                        .mediaType("IMAGE")
                        .uploadedAt(LocalDateTime.now())
                        .vehicle(savedVehicle)
                        .build();

                mediaRepository.save(media);
            }
        }

        // VIDEO UPLOAD
        if (videos != null && !videos.isEmpty()) {
            long maxVideoSize = 250 * 1024 * 1024; // 250 MB

            for (MultipartFile file : videos) {
                String contentType = file.getContentType();
                if (contentType == null || !(contentType.equalsIgnoreCase("video/mp4")
                        || contentType.equalsIgnoreCase("video/quicktime")
                        || contentType.equalsIgnoreCase("video/x-msvideo"))) {
                    throw new RuntimeException("Only MP4, MOV, and AVI videos are allowed");
                }
                if (file.isEmpty()) {
                    throw new RuntimeException("Video file cannot be empty");
                }
                if (file.getSize() > maxVideoSize) {
                    throw new RuntimeException("Video size cannot exceed 250 MB");
                }

                String videoFolder = uploadDir + "/dealer_" + dealerId + "/vehicle_" + savedVehicle.getId() + "/videos";
                File folder = new File(videoFolder);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String safeOriginalName = sanitizeFileName(file.getOriginalFilename());
                String fileName = System.currentTimeMillis() + "_" + safeOriginalName;
                Path targetLocation = Paths.get(videoFolder).resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                String publicFilePath = "/" + stripLeadingSlash(uploadDir) + "/dealer_" + dealerId + "/vehicle_" + savedVehicle.getId() + "/videos/" + fileName;

                VehicleMedia media = VehicleMedia.builder()
                        .fileName(fileName)
                        .fileType(file.getContentType())
                        .filePath(publicFilePath)
                        .mediaType("VIDEO")
                        .uploadedAt(LocalDateTime.now())
                        .vehicle(savedVehicle)
                        .build();

                mediaRepository.save(media);
            }
        }

        // Fetch fresh copy with media list populated
        Vehicle freshVehicle = vehicleRepository.findById(savedVehicle.getId()).orElse(savedVehicle);
        return mapToDTO(freshVehicle, null);
    }

    private String stripLeadingSlash(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll("^/+", "");
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file";
        }
        return originalFileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(Long id, VehicleRequestDTO request) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Vehicle not found with id: " + id));

        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setVariant(request.getVariant() != null ? request.getVariant() : "");
        vehicle.setRegistrationYear(request.getRegistrationYear());
        vehicle.setFuelType(request.getFuelType().trim().toUpperCase());
        vehicle.setKilometerDriven(request.getKilometerDriven());
        vehicle.setCity(request.getCity());
        vehicle.setOwnershipDetails(request.getOwnershipDetails());
        vehicle.setAskingPrice(request.getAskingPrice());
        vehicle.setFinanceAvailability(request.getFinanceAvailability());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setVehicleDescription(request.getVehicleDescription().trim().replaceAll("\\s+", " "));

        // Certification request update on edit
        if (Boolean.TRUE.equals(request.getCertifyRequested()) && !Boolean.TRUE.equals(vehicle.getIsCertified())) {
            vehicle.setCertifyRequested(true);
            vehicle.setInspectionStatus(InspectionStatus.PENDING);
            inspectorService.createOrUpdateInspectionRequestForVehicle(vehicle.getId(), vehicle.getDealer().getId());
        }

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return mapToDTO(updatedVehicle, null);
    }

    @Override
    public VehicleResponseDTO updateVehicleStatus(Long id, VehicleStatusRequestDTO request) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Vehicle not found"));

        if (!request.getStatus().equalsIgnoreCase("ACTIVE")
                && !request.getStatus().equalsIgnoreCase("INACTIVE") && !request.getStatus().equalsIgnoreCase("FEATURED")) {
            throw new RuntimeException("Status must be ACTIVE or INACTIVE");
        }

        vehicle.setVehicleStatus(VehicleStatus.valueOf(request.getStatus().toUpperCase()));
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponseDTO.builder()
                .id(updatedVehicle.getId())
                .vehicleStatus(updatedVehicle.getVehicleStatus())
                .build();
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Vehicle not found with id: " + id));

        // Delete dependent records using JPA repositories to prevent foreign key constraint violations
        inspectionRequestRepository.deleteByVehicleId(vehicle.getId());
        instagramPostBatchItemRepository.deleteByVehicleId(vehicle.getId());
        socialPostBatchItemRepository.deleteByVehicleId(vehicle.getId());
        vehicleInstagramPostRequestRepository.deleteByVehicleId(vehicle.getId());
        vehicleSocialPostRequestRepository.deleteByVehicleId(vehicle.getId());
        socialVisitLogRepository.deleteByVehicleId(vehicle.getId());
        wishlistRepository.deleteByVehicleId(vehicle.getId());
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
                .map(v -> mapToDTO(v, null))
                .toList();
    }

    @Override
    public VehicleResponseDTO getVehicleById(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id : " + vehicleId));

        return mapToDTO(vehicle, null);
    }

    @Override
    public List<VehicleResponseDTO> getAllNonPremiumVehicle(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllActiveAndFeaturedVehicles(VehicleType.NON_PREMIUM);

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles found");
        }

        return vehicles.stream()
                .map(v -> mapToDTO(v, customerId))
                .toList();
    }

    @Override
    public List<VehicleResponseDTO> getAllPremiumVehicle(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllActiveAndFeaturedVehicles(VehicleType.PREMIUM);

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles found");
        }

        return vehicles.stream()
                .map(v -> mapToDTO(v, customerId))
                .toList();
    }

    @Override
    public List<VehicleResponseDTO> getAllVerifiedNonPremiumVehicle(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllActiveVerifiedVehicles(VehicleType.NON_PREMIUM);
        return vehicles.stream()
                .map(v -> mapToDTO(v, customerId))
                .toList();
    }

    @Override
    public List<VehicleResponseDTO> getAllVerifiedPremiumVehicle(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findAllActiveVerifiedVehicles(VehicleType.PREMIUM);
        return vehicles.stream()
                .map(v -> mapToDTO(v, customerId))
                .toList();
    }

    @Override
    public List<VehicleResponseDTO> getLatestFeaturedVehicles(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findTop10ByVehicleStatusOrderByIdDesc(VehicleStatus.FEATURED);
        return vehicles.stream()
                .map(v -> mapToDTO(v, customerId))
                .toList();
    }

    @Override
    public List<VehicleResponseDTO> getLatestVehicles(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findTop10ByVehicleStatusInOrderByCreatedAtDesc(
                List.of(VehicleStatus.ACTIVE, VehicleStatus.FEATURED)
        );

        return vehicles.stream()
                .filter(vehicle -> vehicle.getVehicleStatus() == VehicleStatus.ACTIVE
                        || vehicle.getVehicleStatus() == VehicleStatus.FEATURED)
                .map(v -> mapToDTO(v, customerId))
                .toList();
    }

    // ==========================================
    // CENTRALIZED DTO MAPPER
    // ==========================================

    private VehicleResponseDTO mapToDTO(Vehicle vehicle, Long customerId) {
        List<String> image = vehicle.getMediaList() == null
                ? List.of()
                : vehicle.getMediaList().stream()
                .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                .map(media -> buildMediaUrl(media.getFilePath()))
                .toList();

        List<String> video = vehicle.getMediaList() == null
                ? List.of()
                : vehicle.getMediaList().stream()
                .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                .map(media -> buildMediaUrl(media.getFilePath()))
                .toList();

        boolean isWish = customerId != null && wishlistRepository.existsByCustomer_IdAndVehicle_Id(customerId, vehicle.getId());

        Dealer d = vehicle.getDealer();

        return VehicleResponseDTO.builder()
                .id(vehicle.getId())
                .dealerId(d != null ? d.getId() : null)
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .variant(vehicle.getVariant())
                .registrationYear(vehicle.getRegistrationYear())
                .askingPrice(vehicle.getAskingPrice() != null ? BigDecimal.valueOf(vehicle.getAskingPrice()) : null)
                .kilometerDriven(vehicle.getKilometerDriven())
                .fuelType(vehicle.getFuelType())
                .ownershipDetails(vehicle.getOwnershipDetails())
                .vehicleDescription(vehicle.getVehicleDescription())
                .city(vehicle.getCity())
                .dealerContactName(d != null ? d.getOwnerName() : null)
                .dealerContactNumber(d != null ? d.getDealerMobile() : null)
                .executiveMobile(d != null ? d.getExecutiveMobile() : null)
                .dealerWhatsappNumber(d != null ? d.getWhatsapp() : null)
                .dealerBusinessName(d != null ? d.getBusinessName() : null)
                .dealerContactEmail(d != null ? d.getEmail() : null)
                .dealerYearsInBusiness(d != null ? d.getYearsInBusiness() : 0)
                .dealerLogo(d != null ? buildMediaUrl(d.getDealerLogo()) : null)
                .dealerShowroomImage(d != null ? buildMediaUrl(d.getShowroomImage()) : null)
                .vehicleStatus(vehicle.getVehicleStatus())
                .vehicleType(vehicle.getVehicleType())
                .createdAt(vehicle.getCreatedAt())
                .financeAvailability(vehicle.isFinanceAvailability())

                // Certification details
                .certifyRequested(vehicle.getCertifyRequested())
                .isCertified(vehicle.getIsCertified())
                .inspectionStatus(vehicle.getInspectionStatus())
                .certifiedAt(vehicle.getCertifiedAt())
                .certifiedByInspectorId(vehicle.getCertifiedBy() != null ? vehicle.getCertifiedBy().getId() : null)
                .certifiedByInspectorName(vehicle.getCertifiedBy() != null ? vehicle.getCertifiedBy().getFullName() : null)
                .inspectionNotes(vehicle.getInspectionNotes())

                .images(image)
                .videos(video)
                .isWishList(isWish)
                .build();
    }

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
}
