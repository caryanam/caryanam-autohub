package com.autohub.serviceImpl;

import com.autohub.configuration.WhatsAppOfferClient;
import com.autohub.dto.DealerOfferBroadcastEvent;
import com.autohub.dto.DealerOfferResponseDTO;
import com.autohub.dto.offer.DealerOfferRequestDTO;
import com.autohub.entity.DealerOffer;
import com.autohub.entity.WhatsappOfferMessageLog;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.DealerOfferRepository;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.WhatsappOfferMessageLogRepository;
import com.autohub.service.DealerOfferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DealerOfferServiceImpl implements DealerOfferService {

    private final DealerOfferRepository dealerOfferRepository;
    private final DealerRepository dealerRepository;
    private final WhatsAppOfferClient whatsAppOfferClient;
    private final ApplicationEventPublisher eventPublisher;

    // Add to constructor parameters
    private final WhatsappOfferMessageLogRepository offerMessageLogRepository;

    // Used to build absolute URLs for images stored on disk, e.g.
    // https://c1.caryanam.com + /uploads/offers/xxx.jpg
    @Value("${spring.server.url}")
    private String serverUrl;

    // Updated constructor
    public DealerOfferServiceImpl(DealerOfferRepository dealerOfferRepository,
                                  DealerRepository dealerRepository,
                                  WhatsAppOfferClient whatsAppOfferClient,
                                  ApplicationEventPublisher eventPublisher,
                                  WhatsappOfferMessageLogRepository offerMessageLogRepository) {
        this.dealerOfferRepository = dealerOfferRepository;
        this.dealerRepository = dealerRepository;
        this.whatsAppOfferClient = whatsAppOfferClient;
        this.eventPublisher = eventPublisher;
        this.offerMessageLogRepository = offerMessageLogRepository;
    }

    @Override
    @Transactional
    public DealerOfferResponseDTO sendOfferToAllDealers(
            DealerOfferRequestDTO requestDTO,
            MultipartFile offerImage,
            Long adminId) {

        // Step 1: Save image to local disk. Returns a clean PUBLIC path
        // (e.g. "/uploads/offers/xxx.jpg"), not an OS file path - this is
        // what gets stored in the DB.
        String imagePublicPath = saveImageLocally(offerImage);

        // Step 2: Upload image to Meta NOW (before transaction commits)
        // We do this inside the transaction so if Meta upload fails,
        // we don't commit a broken offer record with no image handle
        String metaImageHandle;
        try {
            metaImageHandle = whatsAppOfferClient.uploadImageToMeta(
                    offerImage.getBytes(),
                    offerImage.getOriginalFilename()
            );
        } catch (Exception ex) {
            log.error("Meta image upload failed, aborting offer creation: {}", ex.getMessage());
            throw new RuntimeException(
                    "Failed to upload offer image to WhatsApp. Please try again.", ex);
        }

        // Step 3: Count active dealers for reporting
        int totalDealers = dealerRepository.findAllActiveDealers().size();

        // Step 4: Persist the offer record
        DealerOffer offer = DealerOffer.builder()
                .offerTitle(requestDTO.getOfferTitle())
                .dealerGreetingName(requestDTO.getDealerGreetingName())
                .offerDetails(requestDTO.getOfferDetails())
                .benefits(requestDTO.getBenefits())
                .contactInfo(requestDTO.getContactInfo())
                .imageUrl(imagePublicPath)
                .metaImageHandle(metaImageHandle)
                .totalDealersTargeted(totalDealers)
                .createdByAdminId(adminId)
                .build();

        DealerOffer saved = dealerOfferRepository.save(offer);

        // Step 5: Publish event — actual WhatsApp sends happen AFTER commit
        eventPublisher.publishEvent(new DealerOfferBroadcastEvent(
                saved.getId(),
                saved.getDealerGreetingName(),
                saved.getOfferDetails(),
                saved.getBenefits(),
                saved.getContactInfo(),
                saved.getMetaImageHandle(),
                totalDealers
        ));

        log.info("Dealer offer [{}] saved and broadcast event published. Targeting [{}] dealers.",
                saved.getId(), totalDealers);

        return DealerOfferResponseDTO.builder()
                .offerId(saved.getId())
                .offerTitle(saved.getOfferTitle())
                .status("BROADCAST_INITIATED")
                .totalDealersTargeted(totalDealers)
                .createdAt(saved.getCreatedAt())
                .message("Offer saved successfully. WhatsApp messages are being sent to "
                        + totalDealers + " active dealers in the background.")
                .build();
    }

    /**
     * Saves the file to disk and returns a clean PUBLIC path (always one
     * leading slash, forward slashes only) rather than an OS Path.toString()
     * value. This is what should be stored in the DB - buildMediaUrl()
     * turns it into an absolute URL whenever it's returned to a client.
     */
    private String saveImageLocally(MultipartFile file) {
        try {
            String uploadDir = "uploads/offers";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            String publicPath = "/" + uploadDir + "/" + filename;

            log.info("Offer image saved locally at: {} (public path: {})", filePath, publicPath);
            return publicPath;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save offer image locally", ex);
        }
    }

    /**
     * Builds a full media URL from a stored path, normalizing it first so a
     * missing leading slash (e.g. from data saved before this fix) can
     * never again produce a broken concatenated URL.
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
    @Transactional(readOnly = true)
    public List<DealerOfferResponseDTO> getAllOffers() {

        return dealerOfferRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(offer -> {

                    // Fetch per-dealer logs for this offer
                    List<WhatsappOfferMessageLog> logs =
                            offerMessageLogRepository.findByOfferIdOrderByCreatedAtAsc(offer.getId());

                    List<DealerOfferResponseDTO.DealerLogEntry> dealerLogs = logs.stream()
                            .map(log -> DealerOfferResponseDTO.DealerLogEntry.builder()
                                    .dealerId(log.getDealerId())
                                    .dealerName(log.getDealerName())
                                    .mobileNumber(log.getMobileNumber())
                                    .status(log.getStatus())
                                    .whatsappMessageId(log.getWhatsappMessageId())
                                    .errorMessage(log.getErrorMessage())
                                    .sentAt(log.getCreatedAt())
                                    .build())
                            .toList();

                    return DealerOfferResponseDTO.builder()
                            .offerId(offer.getId())
                            .offerTitle(offer.getOfferTitle())
                            .dealerGreetingName(offer.getDealerGreetingName())
                            .offerDetails(offer.getOfferDetails())
                            .benefits(offer.getBenefits())
                            .contactInfo(offer.getContactInfo())
                            .imageUrl(buildMediaUrl(offer.getImageUrl()))
                            .totalDealersTargeted(offer.getTotalDealersTargeted())
                            .totalSentSuccess(offer.getTotalSentSuccess())
                            .totalSentFailed(offer.getTotalSentFailed())
                            .createdAt(offer.getCreatedAt())
                            .dealerLogs(dealerLogs)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DealerOfferResponseDTO getOfferById(Long offerId) {

        DealerOffer offer = dealerOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offer not found with id: " + offerId));

        List<WhatsappOfferMessageLog> logs =
                offerMessageLogRepository.findByOfferIdOrderByCreatedAtAsc(offerId);

        List<DealerOfferResponseDTO.DealerLogEntry> dealerLogs = logs.stream()
                .map(log -> DealerOfferResponseDTO.DealerLogEntry.builder()
                        .dealerId(log.getDealerId())
                        .dealerName(log.getDealerName())
                        .mobileNumber(log.getMobileNumber())
                        .status(log.getStatus())
                        .whatsappMessageId(log.getWhatsappMessageId())
                        .errorMessage(log.getErrorMessage())
                        .sentAt(log.getCreatedAt())
                        .build())
                .toList();

        return DealerOfferResponseDTO.builder()
                .offerId(offer.getId())
                .offerTitle(offer.getOfferTitle())
                .dealerGreetingName(offer.getDealerGreetingName())
                .offerDetails(offer.getOfferDetails())
                .benefits(offer.getBenefits())
                .contactInfo(offer.getContactInfo())
                .imageUrl(buildMediaUrl(offer.getImageUrl()))
                .totalDealersTargeted(offer.getTotalDealersTargeted())
                .totalSentSuccess(offer.getTotalSentSuccess())
                .totalSentFailed(offer.getTotalSentFailed())
                .createdAt(offer.getCreatedAt())
                .dealerLogs(dealerLogs)
                .build();
    }
}






//Old Method


//import com.autohub.configuration.WhatsAppOfferClient;
//import com.autohub.dto.DealerOfferBroadcastEvent;
//import com.autohub.dto.DealerOfferResponseDTO;
//import com.autohub.dto.offer.DealerOfferRequestDTO;
//import com.autohub.entity.DealerOffer;
//import com.autohub.entity.WhatsappOfferMessageLog;
//import com.autohub.exception.ResourceNotFoundException;
//import com.autohub.repository.DealerOfferRepository;
//import com.autohub.repository.DealerRepository;
//import com.autohub.repository.WhatsappOfferMessageLogRepository;
//import com.autohub.service.DealerOfferService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@Slf4j
//public class DealerOfferServiceImpl implements DealerOfferService {
//
//    private final DealerOfferRepository dealerOfferRepository;
//    private final DealerRepository dealerRepository;
//    private final WhatsAppOfferClient whatsAppOfferClient;
//    private final ApplicationEventPublisher eventPublisher;
//
//    // Add to constructor parameters
//    private final WhatsappOfferMessageLogRepository offerMessageLogRepository;
//
//    // Updated constructor
//    public DealerOfferServiceImpl(DealerOfferRepository dealerOfferRepository,
//                                  DealerRepository dealerRepository,
//                                  WhatsAppOfferClient whatsAppOfferClient,
//                                  ApplicationEventPublisher eventPublisher,
//                                  WhatsappOfferMessageLogRepository offerMessageLogRepository) {
//        this.dealerOfferRepository = dealerOfferRepository;
//        this.dealerRepository = dealerRepository;
//        this.whatsAppOfferClient = whatsAppOfferClient;
//        this.eventPublisher = eventPublisher;
//        this.offerMessageLogRepository = offerMessageLogRepository;
//    }
//
//    @Override
//    @Transactional
//    public DealerOfferResponseDTO sendOfferToAllDealers(
//            DealerOfferRequestDTO requestDTO,
//            MultipartFile offerImage,
//            Long adminId) {
//
//        // Step 1: Save image to local disk
//        String imageUrl = saveImageLocally(offerImage);
//
//        // Step 2: Upload image to Meta NOW (before transaction commits)
//        // We do this inside the transaction so if Meta upload fails,
//        // we don't commit a broken offer record with no image handle
//        String metaImageHandle;
//        try {
//            metaImageHandle = whatsAppOfferClient.uploadImageToMeta(
//                    offerImage.getBytes(),
//                    offerImage.getOriginalFilename()
//            );
//        } catch (Exception ex) {
//            log.error("Meta image upload failed, aborting offer creation: {}", ex.getMessage());
//            throw new RuntimeException(
//                    "Failed to upload offer image to WhatsApp. Please try again.", ex);
//        }
//
//        // Step 3: Count active dealers for reporting
//        int totalDealers = dealerRepository.findAllActiveDealers().size();
//
//        // Step 4: Persist the offer record
//        DealerOffer offer = DealerOffer.builder()
//                .offerTitle(requestDTO.getOfferTitle())
//                .dealerGreetingName(requestDTO.getDealerGreetingName())
//                .offerDetails(requestDTO.getOfferDetails())
//                .benefits(requestDTO.getBenefits())
//                .contactInfo(requestDTO.getContactInfo())
//                .imageUrl(imageUrl)
//                .metaImageHandle(metaImageHandle)
//                .totalDealersTargeted(totalDealers)
//                .createdByAdminId(adminId)
//                .build();
//
//        DealerOffer saved = dealerOfferRepository.save(offer);
//
//        // Step 5: Publish event — actual WhatsApp sends happen AFTER commit
//        eventPublisher.publishEvent(new DealerOfferBroadcastEvent(
//                saved.getId(),
//                saved.getDealerGreetingName(),
//                saved.getOfferDetails(),
//                saved.getBenefits(),
//                saved.getContactInfo(),
//                saved.getMetaImageHandle(),
//                totalDealers
//        ));
//
//        log.info("Dealer offer [{}] saved and broadcast event published. Targeting [{}] dealers.",
//                saved.getId(), totalDealers);
//
//        return DealerOfferResponseDTO.builder()
//                .offerId(saved.getId())
//                .offerTitle(saved.getOfferTitle())
//                .status("BROADCAST_INITIATED")
//                .totalDealersTargeted(totalDealers)
//                .createdAt(saved.getCreatedAt())
//                .message("Offer saved successfully. WhatsApp messages are being sent to "
//                        + totalDealers + " active dealers in the background.")
//                .build();
//    }
//
//    private String saveImageLocally(MultipartFile file) {
//        try {
//            String uploadDir = "uploads/offers/";
//            Path uploadPath = Paths.get(uploadDir);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            Path filePath = uploadPath.resolve(filename);
//            Files.copy(file.getInputStream(), filePath);
//            log.info("Offer image saved locally at: {}", filePath);
//            return filePath.toString();
//        } catch (IOException ex) {
//            throw new RuntimeException("Failed to save offer image locally", ex);
//        }
//    }
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<DealerOfferResponseDTO> getAllOffers() {
//
//        return dealerOfferRepository.findAllByOrderByCreatedAtDesc()
//                .stream()
//                .map(offer -> {
//
//                    // Fetch per-dealer logs for this offer
//                    List<WhatsappOfferMessageLog> logs =
//                            offerMessageLogRepository.findByOfferIdOrderByCreatedAtAsc(offer.getId());
//
//                    List<DealerOfferResponseDTO.DealerLogEntry> dealerLogs = logs.stream()
//                            .map(log -> DealerOfferResponseDTO.DealerLogEntry.builder()
//                                    .dealerId(log.getDealerId())
//                                    .dealerName(log.getDealerName())
//                                    .mobileNumber(log.getMobileNumber())
//                                    .status(log.getStatus())
//                                    .whatsappMessageId(log.getWhatsappMessageId())
//                                    .errorMessage(log.getErrorMessage())
//                                    .sentAt(log.getCreatedAt())
//                                    .build())
//                            .toList();
//
//                    return DealerOfferResponseDTO.builder()
//                            .offerId(offer.getId())
//                            .offerTitle(offer.getOfferTitle())
//                            .dealerGreetingName(offer.getDealerGreetingName())
//                            .offerDetails(offer.getOfferDetails())
//                            .benefits(offer.getBenefits())
//                            .contactInfo(offer.getContactInfo())
//                            .imageUrl(offer.getImageUrl())
//                            .totalDealersTargeted(offer.getTotalDealersTargeted())
//                            .totalSentSuccess(offer.getTotalSentSuccess())
//                            .totalSentFailed(offer.getTotalSentFailed())
//                            .createdAt(offer.getCreatedAt())
//                            .dealerLogs(dealerLogs)
//                            .build();
//                })
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public DealerOfferResponseDTO getOfferById(Long offerId) {
//
//        DealerOffer offer = dealerOfferRepository.findById(offerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Offer not found with id: " + offerId));
//
//        List<WhatsappOfferMessageLog> logs =
//                offerMessageLogRepository.findByOfferIdOrderByCreatedAtAsc(offerId);
//
//        List<DealerOfferResponseDTO.DealerLogEntry> dealerLogs = logs.stream()
//                .map(log -> DealerOfferResponseDTO.DealerLogEntry.builder()
//                        .dealerId(log.getDealerId())
//                        .dealerName(log.getDealerName())
//                        .mobileNumber(log.getMobileNumber())
//                        .status(log.getStatus())
//                        .whatsappMessageId(log.getWhatsappMessageId())
//                        .errorMessage(log.getErrorMessage())
//                        .sentAt(log.getCreatedAt())
//                        .build())
//                .toList();
//
//        return DealerOfferResponseDTO.builder()
//                .offerId(offer.getId())
//                .offerTitle(offer.getOfferTitle())
//                .dealerGreetingName(offer.getDealerGreetingName())
//                .offerDetails(offer.getOfferDetails())
//                .benefits(offer.getBenefits())
//                .contactInfo(offer.getContactInfo())
//                .imageUrl(offer.getImageUrl())
//                .totalDealersTargeted(offer.getTotalDealersTargeted())
//                .totalSentSuccess(offer.getTotalSentSuccess())
//                .totalSentFailed(offer.getTotalSentFailed())
//                .createdAt(offer.getCreatedAt())
//                .dealerLogs(dealerLogs)
//                .build();
//    }
//}
