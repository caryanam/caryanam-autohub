package com.autohub.serviceImpl;

import com.autohub.configuration.WhatsAppVehicleClient;
import com.autohub.dto.VehicleShareResponseDTO;
import com.autohub.dto.WhatsAppProperties;
import com.autohub.entity.*;
import com.autohub.enums.WhatsappMessageStatus;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.VehicleWhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class VehicleWhatsAppServiceImpl implements VehicleWhatsAppService {

    private final VehicleRepository vehicleRepository;
    private final DealerRepository dealerRepository;
    private final VehicleMediaRepository mediaRepository;
    private final WhatsAppVehicleClient whatsAppVehicleClient;
    private final WhatsappVehicleShareLogRepository shareLogRepository;
    private final WhatsAppProperties properties;

    public VehicleWhatsAppServiceImpl(
            VehicleRepository vehicleRepository,
            DealerRepository dealerRepository,
            VehicleMediaRepository mediaRepository,
            WhatsAppVehicleClient whatsAppVehicleClient,
            WhatsappVehicleShareLogRepository shareLogRepository,
            WhatsAppProperties properties) {
        this.vehicleRepository = vehicleRepository;
        this.dealerRepository = dealerRepository;
        this.mediaRepository = mediaRepository;
        this.whatsAppVehicleClient = whatsAppVehicleClient;
        this.shareLogRepository = shareLogRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public VehicleShareResponseDTO shareVehicleOnWhatsApp(Long vehicleId, Long dealerId) {
        // Backward compatibility: delegate to the new method with shareToSelf = true
        com.autohub.dto.ShareVehicleRequestDTO request = new com.autohub.dto.ShareVehicleRequestDTO();
        request.setDealerId(dealerId);
        request.setShareToSelf(true);
        List<VehicleShareResponseDTO> responses = shareVehicleOnWhatsApp(vehicleId, request);
        return responses.isEmpty() ? null : responses.get(0);
    }

    @Override
    @Transactional
    public List<VehicleShareResponseDTO> shareVehicleOnWhatsApp(Long vehicleId,
            com.autohub.dto.ShareVehicleRequestDTO request) {

        Long dealerId = request.getDealerId();
        log.info(
                "Vehicle WhatsApp share initiated → vehicleId=[{}] dealerId=[{}] shareToSelf=[{}] customerWhatsapp=[{}]",
                vehicleId, dealerId, request.isShareToSelf(), request.getCustomerWhatsapp());

        if (!request.isShareToSelf() && !StringUtils.hasText(request.getCustomerWhatsapp())) {
            throw new RuntimeException(
                    "Please select at least one recipient (share to self or provide customer WhatsApp number).");
        }

        // ── Step 1: Validate vehicle exists and belongs to this dealer ──
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + vehicleId));

        if (!vehicle.getDealer().getId().equals(dealerId)) {
            throw new RuntimeException(
                    "Access denied. This vehicle does not belong to your account.");
        }

        // ── Step 2: Validate dealer exists ──
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dealer not found with id: " + dealerId));

        // Determine recipients
        record Recipient(String number, com.autohub.enums.ShareMode shareType) {
        }
        java.util.List<Recipient> recipients = new java.util.ArrayList<>();

        if (request.isShareToSelf()) {
            if (!StringUtils.hasText(dealer.getWhatsapp())) {
                throw new RuntimeException(
                        "Your WhatsApp number is not configured. Please update your profile with a valid WhatsApp number.");
            }
            String dealerWhatsApp = normalizeToE164(dealer.getWhatsapp());
            if (dealerWhatsApp == null) {
                throw new RuntimeException(
                        "Your WhatsApp number '" + dealer.getWhatsapp() + "' is in an unrecognized format.");
            }
            recipients.add(new Recipient(dealerWhatsApp, com.autohub.enums.ShareMode.SELF));
        }

        if (StringUtils.hasText(request.getCustomerWhatsapp())) {
            String customerWhatsApp = normalizeToE164(request.getCustomerWhatsapp());
            if (customerWhatsApp == null) {
                throw new RuntimeException("Customer WhatsApp number '" + request.getCustomerWhatsapp()
                        + "' is in an unrecognized format.");
            }
            recipients.add(new Recipient(customerWhatsApp, com.autohub.enums.ShareMode.CUSTOMER));
        }

        // ── Step 3: Get first image of this vehicle from DB ──
        List<VehicleMedia> mediaList = mediaRepository.findByVehicleId(vehicleId);

        VehicleMedia firstImage = mediaList.stream()
                .filter(m -> "IMAGE".equalsIgnoreCase(m.getMediaType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No images found for this vehicle. " +
                                "Please upload at least one image before sharing."));

        // ── Step 4: Read image file from disk and upload to Meta ──
        String mediaId;
        try {
            // filePath stored in DB e.g. /uploads/dealers/dealer_1/vehicle_5/images/xyz.jpg
            // Strip leading slash for Path resolution
            String cleanPath = firstImage.getFilePath().replaceAll("^/+", "");
            Path imagePath = Paths.get(cleanPath);

            if (!Files.exists(imagePath)) {
                throw new RuntimeException(
                        "Vehicle image file not found on server: " + cleanPath);
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String mimeType = resolveMimeType(firstImage.getFileName());

            mediaId = whatsAppVehicleClient.uploadVehicleImage(
                    imageBytes,
                    firstImage.getFileName(),
                    mimeType);

        } catch (IOException ex) {
            log.error("Failed to read vehicle image from disk for vehicleId=[{}]: {}",
                    vehicleId, ex.getMessage());
            throw new RuntimeException(
                    "Could not read vehicle image. Please try again.", ex);
        }

        // ── Step 5: Build template variables from vehicle data ──
        String vehicleName = buildVehicleName(vehicle);
        String price = formatPrice(vehicle.getAskingPrice());
        String regYear = String.valueOf(vehicle.getRegistrationYear());
        String fuelType = vehicle.getFuelType();
        String kmDriven = formatKm(Math.toIntExact(vehicle.getKilometerDriven()));
        String location = vehicle.getCity();
        String specs = buildSpecifications(vehicle);
        String description = vehicle.getVehicleDescription();

        java.util.List<VehicleShareResponseDTO> responses = new java.util.ArrayList<>();

        // ── Step 6: Send template message to each recipient ──
        for (Recipient recipient : recipients) {
            log.info("Sending vehicle catalog → to=[{}] type=[{}] vehicle=[{}] mediaId=[{}]",
                    recipient.number(), recipient.shareType(), vehicleName, mediaId);

            WhatsAppVehicleClient.VehicleShareResult result = whatsAppVehicleClient.sendVehicleCatalogTemplate(
                    recipient.number(),
                    mediaId,
                    vehicleName,
                    price,
                    regYear,
                    fuelType,
                    kmDriven,
                    location,
                    specs,
                    description);

            // ── Step 7: Persist log regardless of outcome ──
            WhatsappVehicleShareLog logEntry = WhatsappVehicleShareLog.builder()
                    .vehicleId(vehicleId)
                    .dealerId(dealerId)
                    .dealerName(dealer.getOwnerName())
                    .sentToNumber(recipient.number())
                    .vehicleDisplayName(vehicleName)
                    .templateName(properties.vehicleTemplateName())
                    .status(result.success()
                            ? WhatsappMessageStatus.SUCCESS
                            : WhatsappMessageStatus.FAILED)
                    .whatsappMessageId(result.whatsappMessageId())
                    .metaImageHandle(mediaId)
                    .responsePayload(result.responsePayload())
                    .errorMessage(result.errorMessage())
                    .shareType(recipient.shareType())
                    .build();

            WhatsappVehicleShareLog saved = shareLogRepository.save(logEntry);

            if (result.success()) {
                log.info("✓ Vehicle shared successfully → vehicleId=[{}] " +
                        "dealerId=[{}] type=[{}] messageId=[{}]",
                        vehicleId, dealerId, recipient.shareType(), result.whatsappMessageId());
            } else {
                log.error("✗ Vehicle share failed → vehicleId=[{}] dealerId=[{}] type=[{}] error=[{}]",
                        vehicleId, dealerId, recipient.shareType(), result.errorMessage());
            }

            responses.add(VehicleShareResponseDTO.builder()
                    .logId(saved.getId())
                    .vehicleId(vehicleId)
                    .vehicleDisplayName(vehicleName)
                    .sentToNumber(recipient.number())
                    .status(saved.getStatus())
                    .deliveryStatus(saved.getDeliveryStatus())
                    .shareType(saved.getShareType())
                    .whatsappMessageId(result.whatsappMessageId())
                    .message(result.success()
                            ? "Vehicle details sent to WhatsApp successfully!"
                            : "Failed to send vehicle details. Error: " + result.errorMessage())
                    .sharedAt(saved.getSharedAt())
                    .build());
        }

        return responses;
    }

    @Override
    public List<VehicleShareResponseDTO> getDealerShareHistory(Long dealerId) {
        return shareLogRepository
                .findByDealerIdOrderBySharedAtDesc(dealerId)
                .stream()
                .map(log -> VehicleShareResponseDTO.builder()
                        .logId(log.getId())
                        .vehicleId(log.getVehicleId())
                        .vehicleDisplayName(log.getVehicleDisplayName())
                        .sentToNumber(log.getSentToNumber())
                        .status(log.getStatus())
                        .deliveryStatus(log.getDeliveryStatus())
                        .whatsappMessageId(log.getWhatsappMessageId())
                        .sharedAt(log.getSharedAt())
                        .message(log.getStatus() == WhatsappMessageStatus.SUCCESS
                                ? "Sent successfully"
                                : "Failed: " + log.getErrorMessage())
                        .build())
                .toList();
    }

    @Override
    public long getVehicleShareCount(Long vehicleId) {
        return shareLogRepository.countByVehicleIdAndStatus(
                vehicleId, WhatsappMessageStatus.SUCCESS);
    }

    // ── Private helpers ──

    private String buildVehicleName(Vehicle vehicle) {
        StringBuilder name = new StringBuilder();
        if (StringUtils.hasText(vehicle.getBrand()))
            name.append(vehicle.getBrand()).append(" ");
        if (StringUtils.hasText(vehicle.getModel()))
            name.append(vehicle.getModel());
        if (StringUtils.hasText(vehicle.getVariant()))
            name.append(" ").append(vehicle.getVariant());
        return name.toString().trim();
    }

    private String formatPrice(double price) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("en", "IN"));
        return "₹" + fmt.format((long) price);
    }

    private String formatKm(int km) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("en", "IN"));
        return fmt.format(km) + " km";
    }

    private String buildSpecifications(Vehicle vehicle) {
        return "Ownership: " + nullSafe(String.valueOf(vehicle.getOwnershipDetails())) +
                ", Category: " + nullSafe(
                        vehicle.getVehicleType() != null
                                ? vehicle.getVehicleType().name()
                                : null)
                +
                ", Status: " + nullSafe(
                        vehicle.getVehicleStatus() != null
                                ? vehicle.getVehicleStatus().name()
                                : null)
                +
                ", Finance: " + (vehicle.isFinanceAvailability() ? "Yes" : "No");
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }

    /**
     * Converts any real-world Indian mobile number format to the
     * E.164 digits-only format Meta requires (e.g. 919876543210).
     *
     * Handles all formats found in practice:
     * 9876543210 → 919876543210 (10 digit clean)
     * 919876543210 → 919876543210 (already correct 12 digit)
     * +919876543210 → 919876543210 (+ prefix)
     * +91 9876543210 → 919876543210 (+ and space)
     * 09876543210 → 919876543210 (leading 0 — ISD habit)
     * 0091 9876543210 → 919876543210 (0091 prefix)
     * 91 98765 43210 → 919876543210 (formatted with spaces)
     * 910 9876543210 → 919876543210 (910 prefix edge case)
     *
     * Returns null if the number cannot be normalized to a valid
     * 12-digit Indian mobile number — caller must throw or skip.
     */
    private String normalizeToE164(String rawNumber) {

        if (rawNumber == null || rawNumber.isBlank()) {
            return null;
        }

        // Strip everything except digits
        String digits = rawNumber.replaceAll("[^0-9]", "");

        // Already correct: 91 + 10 digit Indian number = 12 digits
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits;
        }

        // Clean 10-digit Indian mobile number — just prefix 91
        if (digits.length() == 10) {
            return "91" + digits;
        }

        // 11 digits with leading 0 (e.g. 09876543210) — strip 0, prefix 91
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }

        // 13 digits with 0091 prefix (e.g. 00919876543210) — strip 00, keep rest
        if (digits.length() == 13 && digits.startsWith("0091")) {
            return digits.substring(2);
        }

        // 13 digits starting with 910 — country code + leading 0 edge case
        if (digits.length() == 13 && digits.startsWith("910")) {
            return "91" + digits.substring(3);
        }

        // Unrecognized format — return null so caller handles it cleanly
        log.warn("Cannot normalize WhatsApp number to E164: '{}' → " +
                "digits='{}' length={}",
                rawNumber, digits, digits.length());
        return null;
    }

    private String resolveMimeType(String filename) {
        if (filename == null)
            return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".webp"))
            return "image/webp";
        return "image/jpeg";
    }
}