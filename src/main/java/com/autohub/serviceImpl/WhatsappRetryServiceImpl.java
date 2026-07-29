package com.autohub.serviceImpl;

import com.autohub.configuration.WhatsAppClient;
import com.autohub.configuration.WhatsAppOfferClient;
import com.autohub.configuration.WhatsAppVehicleClient;
import com.autohub.dto.WhatsAppProperties;
import com.autohub.dto.WhatsAppTemplateRequest;
import com.autohub.dto.WhatsappDashboardStatsDTO;
import com.autohub.entity.*;
import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.enums.WhatsappMessageStatus;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.WhatsappRetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class WhatsappRetryServiceImpl implements WhatsappRetryService {

    private static final int MAX_RETRY_COUNT = 3;

    private final WhatsappMessageLogRepository messageLogRepository;
    private final WhatsappOfferMessageLogRepository offerMessageLogRepository;
    private final WhatsappVehicleShareLogRepository vehicleShareLogRepository;
    private final WhatsAppClient whatsAppClient;
    private final WhatsAppOfferClient offerClient;
    private final WhatsAppVehicleClient vehicleClient;
    private final DealerOfferRepository dealerOfferRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMediaRepository mediaRepository;
    private final WhatsAppProperties properties;

    public WhatsappRetryServiceImpl(
            WhatsappMessageLogRepository messageLogRepository,
            WhatsappOfferMessageLogRepository offerMessageLogRepository,
            WhatsappVehicleShareLogRepository vehicleShareLogRepository,
            WhatsAppClient whatsAppClient,
            WhatsAppOfferClient offerClient,
            WhatsAppVehicleClient vehicleClient,
            DealerOfferRepository dealerOfferRepository,
            VehicleRepository vehicleRepository,
            VehicleMediaRepository mediaRepository,
            WhatsAppProperties properties) {
        this.messageLogRepository = messageLogRepository;
        this.offerMessageLogRepository = offerMessageLogRepository;
        this.vehicleShareLogRepository = vehicleShareLogRepository;
        this.whatsAppClient = whatsAppClient;
        this.offerClient = offerClient;
        this.vehicleClient = vehicleClient;
        this.dealerOfferRepository = dealerOfferRepository;
        this.vehicleRepository = vehicleRepository;
        this.mediaRepository = mediaRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public WhatsappDashboardStatsDTO.RetryResultDTO retryFailedMessage(
            String logType, Long logId) {

        log.info("Retry requested → logType=[{}] logId=[{}]", logType, logId);

        return switch (logType.toUpperCase()) {
            case "LEAD"    -> retryLeadMessage(logId);
            case "OFFER"   -> retryOfferMessage(logId);
            case "VEHICLE" -> retryVehicleMessage(logId);
            default -> throw new IllegalArgumentException(
                    "Unknown logType: '" + logType + "'. Must be LEAD, OFFER, or VEHICLE");
        };
    }

    // ──────────────────────────────────────────────────
    // LEAD retry
    // ──────────────────────────────────────────────────

    private WhatsappDashboardStatsDTO.RetryResultDTO retryLeadMessage(Long logId) {

        WhatsappMessageLog log = messageLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lead message log not found: " + logId));

        validateRetryEligible(log.getRetryCount(), logId, "LEAD");

        // Increment retry count before attempting
        log.setRetryCount(log.getRetryCount() + 1);
        log.setLastRetryAt(LocalDateTime.now());

        // Rebuild the template request from stored data
        // The mobile number is already stored in log
        WhatsAppTemplateRequest request = WhatsAppTemplateRequest.forNewLead(
                log.getMobileNumber(),
                properties.templateName(),
                properties.languageCode(),
                extractParam(log.getRequestPayload(), 1),  // customerName
                extractParam(log.getRequestPayload(), 2),  // customerMobile
                extractParam(log.getRequestPayload(), 3)   // vehicleName
        );

        try {
            WhatsAppClient.WhatsAppApiCallResult result =
                    whatsAppClient.sendTemplateMessage(request);

            if (result.success()) {
                log.setStatus(WhatsappMessageStatus.SUCCESS);
                log.setDeliveryStatus(WhatsappDeliveryStatus.ACCEPTED);
                log.setWhatsappMessageId(result.whatsappMessageId());
                log.setResponsePayload(result.responsePayload());
                messageLogRepository.save(log);

                return buildRetryResult(logId, "LEAD", true,
                        result.whatsappMessageId(), log.getRetryCount(),
                        "Retry successful — message sent to Meta");
            } else {
                log.setStatus(WhatsappMessageStatus.FAILED);
                messageLogRepository.save(log);
                return buildRetryResult(logId, "LEAD", false, null,
                        log.getRetryCount(), "Retry failed: " + result.errorMessage());
            }
        } catch (Exception ex) {
            log.setStatus(WhatsappMessageStatus.FAILED);
            messageLogRepository.save(log);
            return buildRetryResult(logId, "LEAD", false, null,
                    log.getRetryCount(), "Retry error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────
    // OFFER retry
    // ──────────────────────────────────────────────────

    private WhatsappDashboardStatsDTO.RetryResultDTO retryOfferMessage(Long logId) {

        WhatsappOfferMessageLog offerLog = offerMessageLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offer message log not found: " + logId));

        validateRetryEligible(offerLog.getRetryCount(), logId, "OFFER");

        offerLog.setRetryCount(offerLog.getRetryCount() + 1);
        offerLog.setLastRetryAt(LocalDateTime.now());

        // Load offer data to rebuild payload
        DealerOffer offer = dealerOfferRepository.findById(offerLog.getOfferId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offer not found: " + offerLog.getOfferId()));

        // Re-upload image to Meta (stored handles expire after 30 days)
        String freshMediaHandle;
        try {
            freshMediaHandle = reUploadOfferImage(offer.getImageUrl());
        } catch (Exception ex) {
            log.warn("Could not re-upload image for retry, using stored handle: {}",
                    ex.getMessage());
            // Fall back to stored handle — may work if not expired
            freshMediaHandle = offerLog.getMetaImageHandle();
        }

        try {
            WhatsAppOfferClient.OfferSendResult result = offerClient.sendOfferTemplate(
                    offerLog.getMobileNumber(),
                    properties.offerTemplateName(),
                    properties.offerLanguageCode(),
                    freshMediaHandle,
                    offerLog.getDealerName(),       // {{1}}
                    offer.getOfferDetails(),        // {{2}}
                    offer.getBenefits(),            // {{3}}
                    offer.getContactInfo()          // {{4}}
            );

            if (result.success()) {
                offerLog.setStatus(WhatsappMessageStatus.SUCCESS);
                offerLog.setDeliveryStatus(WhatsappDeliveryStatus.ACCEPTED);
                offerLog.setWhatsappMessageId(result.whatsappMessageId());
                offerLog.setMetaImageHandle(freshMediaHandle);
                offerLog.setResponsePayload(result.responsePayload());
                offerMessageLogRepository.save(offerLog);

                return buildRetryResult(logId, "OFFER", true,
                        result.whatsappMessageId(), offerLog.getRetryCount(),
                        "Retry successful — offer resent to " + offerLog.getDealerName());
            } else {
                offerLog.setStatus(WhatsappMessageStatus.FAILED);
                offerMessageLogRepository.save(offerLog);
                return buildRetryResult(logId, "OFFER", false, null,
                        offerLog.getRetryCount(), "Retry failed: " + result.errorMessage());
            }
        } catch (Exception ex) {
            offerLog.setStatus(WhatsappMessageStatus.FAILED);
            offerMessageLogRepository.save(offerLog);
            return buildRetryResult(logId, "OFFER", false, null,
                    offerLog.getRetryCount(), "Retry error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────
    // VEHICLE retry
    // ──────────────────────────────────────────────────

    private WhatsappDashboardStatsDTO.RetryResultDTO retryVehicleMessage(Long logId) {

        WhatsappVehicleShareLog vehicleLog = vehicleShareLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle share log not found: " + logId));

        validateRetryEligible(vehicleLog.getRetryCount(), logId, "VEHICLE");

        vehicleLog.setRetryCount(vehicleLog.getRetryCount() + 1);
        vehicleLog.setLastRetryAt(LocalDateTime.now());

        // Load vehicle to rebuild payload
        Vehicle vehicle = vehicleRepository.findById(vehicleLog.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found: " + vehicleLog.getVehicleId()));

        // Re-upload first image — handles expire
        String freshMediaHandle;
        try {
            List<VehicleMedia> mediaList = mediaRepository.findByVehicleId(vehicleLog.getVehicleId());
            VehicleMedia firstImage = mediaList.stream()
                    .filter(m -> "IMAGE".equalsIgnoreCase(m.getMediaType()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No image found for vehicle"));

            String cleanPath = firstImage.getFilePath().replaceAll("^/+", "");
            byte[] imageBytes = Files.readAllBytes(Paths.get(cleanPath));
            String mimeType = firstImage.getFileName().toLowerCase().endsWith(".png")
                    ? "image/png" : "image/jpeg";

            freshMediaHandle = vehicleClient.uploadVehicleImage(
                    imageBytes, firstImage.getFileName(), mimeType);
        } catch (Exception ex) {
            log.warn("Could not re-upload vehicle image for retry, using stored handle: {}",
                    ex.getMessage());
            freshMediaHandle = vehicleLog.getMetaImageHandle();
        }

        try {
            WhatsAppVehicleClient.VehicleShareResult result =
                    vehicleClient.sendVehicleCatalogTemplate(
                            vehicleLog.getSentToNumber(),
                            freshMediaHandle,
                            vehicleLog.getVehicleDisplayName(),
                            "₹" + vehicle.getAskingPrice().longValue(),
                            String.valueOf(vehicle.getRegistrationYear()),
                            vehicle.getFuelType(),
                            vehicle.getKilometerDriven() + " km",
                            vehicle.getCity(),
                            "Ownership: " + vehicle.getOwnershipDetails(),
                            vehicle.getVehicleDescription()
                    );

            if (result.success()) {
                vehicleLog.setStatus(WhatsappMessageStatus.SUCCESS);
                vehicleLog.setDeliveryStatus(WhatsappDeliveryStatus.ACCEPTED);
                vehicleLog.setWhatsappMessageId(result.whatsappMessageId());
                vehicleLog.setMetaImageHandle(freshMediaHandle);
                vehicleLog.setResponsePayload(result.responsePayload());
                vehicleShareLogRepository.save(vehicleLog);

                return buildRetryResult(logId, "VEHICLE", true,
                        result.whatsappMessageId(), vehicleLog.getRetryCount(),
                        "Retry successful — vehicle details resent");
            } else {
                vehicleLog.setStatus(WhatsappMessageStatus.FAILED);
                vehicleShareLogRepository.save(vehicleLog);
                return buildRetryResult(logId, "VEHICLE", false, null,
                        vehicleLog.getRetryCount(), "Retry failed: " + result.errorMessage());
            }
        } catch (Exception ex) {
            vehicleLog.setStatus(WhatsappMessageStatus.FAILED);
            vehicleShareLogRepository.save(vehicleLog);
            return buildRetryResult(logId, "VEHICLE", false, null,
                    vehicleLog.getRetryCount(), "Retry error: " + ex.getMessage());
        }
    }

    // ──────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────

    private void validateRetryEligible(int currentRetryCount, Long logId, String type) {
        if (currentRetryCount >= MAX_RETRY_COUNT) {
            throw new RuntimeException(
                    type + " message [" + logId + "] has reached maximum retry limit of "
                            + MAX_RETRY_COUNT + ". Manual intervention required.");
        }
    }

    private WhatsappDashboardStatsDTO.RetryResultDTO buildRetryResult(
            Long logId, String logType, boolean success,
            String messageId, int retryCount, String message) {
        return WhatsappDashboardStatsDTO.RetryResultDTO.builder()
                .logId(logId)
                .logType(logType)
                .success(success)
                .whatsappMessageId(messageId)
                .retryCount(retryCount)
                .message(message)
                .build();
    }

    /**
     * Extracts a parameter value from stored request JSON payload.
     * Falls back to empty string if parsing fails.
     */
    private String extractParam(String requestPayload, int paramIndex) {
        try {
            // Simple string extraction — payload is JSON with parameters array
            String marker = "\"text\":\"";
            int occurrences = 0;
            int pos = 0;
            while (pos < requestPayload.length()) {
                int found = requestPayload.indexOf(marker, pos);
                if (found == -1) break;
                occurrences++;
                if (occurrences == paramIndex) {
                    int start = found + marker.length();
                    int end = requestPayload.indexOf("\"", start);
                    if (end > start) return requestPayload.substring(start, end);
                }
                pos = found + marker.length();
            }
        } catch (Exception ex) {
            log.warn("Could not extract param {} from payload", paramIndex);
        }
        return "";
    }

    private String reUploadOfferImage(String imageUrl) throws IOException {
        String cleanPath = imageUrl.replaceAll("^/+", "");
        Path imagePath = Paths.get(cleanPath);
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String filename = imagePath.getFileName().toString();
        return offerClient.uploadImageToMeta(imageBytes, filename);
    }
}