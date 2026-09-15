package com.autohub.serviceImpl;

import com.autohub.dto.FestivalOfferResponseDTO;
import com.autohub.entity.FestivalOffer;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.FestivalOfferRepository;
import com.autohub.service.FestivalOfferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FestivalOfferServiceImpl implements FestivalOfferService {

    private final FestivalOfferRepository festivalOfferRepository;

    @Value("${spring.server.url}")
    private String serverUrl;

    private static final String UPLOAD_DIR = "uploads/festival-offers";

    public FestivalOfferServiceImpl(FestivalOfferRepository festivalOfferRepository) {
        this.festivalOfferRepository = festivalOfferRepository;
    }

    @Override
    @Transactional
    public FestivalOfferResponseDTO uploadFestivalOffer(MultipartFile file, Integer durationHours) {
        // Determine type
        String contentType = file.getContentType();
        FestivalOffer.MediaType mediaType = FestivalOffer.MediaType.IMAGE;
        if (contentType != null && contentType.startsWith("video/")) {
            mediaType = FestivalOffer.MediaType.VIDEO;
        }

        // Save file
        String publicPath = saveMediaLocally(file);

        // Delete all existing festival offers to keep only 1 active
        List<FestivalOffer> existingOffers = festivalOfferRepository.findAll();
        for (FestivalOffer existing : existingOffers) {
            deleteFestivalOffer(existing.getId());
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(durationHours);

        FestivalOffer entity = FestivalOffer.builder()
                .mediaUrl(publicPath)
                .mediaType(mediaType)
                .enabled(true)
                .expiresAt(expiresAt)
                .build();

        FestivalOffer saved = festivalOfferRepository.save(entity);
        log.info("Festival offer uploaded and enabled: id={}, expiresAt={}", saved.getId(), saved.getExpiresAt());

        return toDTO(saved);
    }

    @Override
    public FestivalOfferResponseDTO getAdminFestivalOffer() {
        return getActiveFestivalOffer();
    }

    @Override
    public FestivalOfferResponseDTO getActiveFestivalOffer() {
        return festivalOfferRepository.findFirstByEnabledTrueOrderByCreatedAtDesc()
                .filter(offer -> offer.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public FestivalOfferResponseDTO disableFestivalOffer(Long id) {
        FestivalOffer offer = festivalOfferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Festival offer not found"));
        offer.setEnabled(false);
        FestivalOffer saved = festivalOfferRepository.save(offer);
        return toDTO(saved);
    }

    // Runs every 1 minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteExpiredOffers() {
        List<FestivalOffer> expiredOffers = festivalOfferRepository.findByExpiresAtBefore(LocalDateTime.now());
        if (!expiredOffers.isEmpty()) {
            log.info("Found {} expired festival offers to delete", expiredOffers.size());
            for (FestivalOffer offer : expiredOffers) {
                deleteFestivalOffer(offer.getId());
            }
        }
    }

    private void deleteFestivalOffer(Long id) {
        FestivalOffer offer = festivalOfferRepository.findById(id).orElse(null);
        if (offer == null) return;

        try {
            String relativePath = offer.getMediaUrl();
            if (relativePath != null && relativePath.startsWith(serverUrl)) {
                relativePath = relativePath.substring(serverUrl.length());
            }
            if (relativePath != null && relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            if (relativePath != null) {
                Path filePath = Paths.get(relativePath);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted festival offer file from disk: {}", filePath);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to delete festival offer file from disk: {}", e.getMessage());
        }

        festivalOfferRepository.delete(offer);
        log.info("Festival offer deleted from database: id={}", id);
    }

    private String saveMediaLocally(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String uniqueName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(uniqueName);
            Files.write(filePath, file.getBytes());

            String publicPath = serverUrl + "/" + UPLOAD_DIR + "/" + uniqueName;
            log.info("Saved festival offer media to disk: {}", publicPath);
            return publicPath;
        } catch (IOException e) {
            log.error("Failed to save festival offer media to disk: {}", e.getMessage());
            throw new RuntimeException("Failed to save festival offer file. Please try again.", e);
        }
    }

    private FestivalOfferResponseDTO toDTO(FestivalOffer entity) {
        return FestivalOfferResponseDTO.builder()
                .id(entity.getId())
                .mediaUrl(entity.getMediaUrl())
                .mediaType(entity.getMediaType())
                .enabled(entity.getEnabled())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
