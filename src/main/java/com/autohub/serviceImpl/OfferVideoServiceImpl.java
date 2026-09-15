package com.autohub.serviceImpl;

import com.autohub.dto.OfferVideoResponseDTO;
import com.autohub.entity.OfferVideo;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.OfferVideoRepository;
import com.autohub.service.OfferVideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OfferVideoServiceImpl implements OfferVideoService {

    private final OfferVideoRepository offerVideoRepository;

    @Value("${spring.server.url}")
    private String serverUrl;

    private static final String UPLOAD_DIR = "uploads/offer-videos";

    public OfferVideoServiceImpl(OfferVideoRepository offerVideoRepository) {
        this.offerVideoRepository = offerVideoRepository;
    }

    @Override
    @Transactional
    public OfferVideoResponseDTO uploadOfferVideo(MultipartFile video) {
        // Step 1: Save video file to disk
        String videoPublicPath = saveVideoLocally(video);

        // Step 2: Delete all existing offer videos
        List<OfferVideo> existingVideos = offerVideoRepository.findAll();
        for (OfferVideo existingVideo : existingVideos) {
            deleteOfferVideo(existingVideo.getId());
        }

        // Step 3: Create and persist entity (enabled by default)
        OfferVideo entity = OfferVideo.builder()
                .videoUrl(videoPublicPath)
                .enabled(true)
                .build();

        OfferVideo saved = offerVideoRepository.save(entity);
        log.info("Offer video uploaded and enabled: id={}", saved.getId());

        return toDTO(saved);
    }

    @Override
    public OfferVideoResponseDTO getAdminOfferVideo() {
        return getActiveOfferVideo();
    }

    @Override
    public OfferVideoResponseDTO getActiveOfferVideo() {
        return offerVideoRepository.findFirstByEnabledTrueOrderByCreatedAtDesc()
                .map(this::toDTO)
                .orElse(null);
    }

    private void deleteOfferVideo(Long id) {
        OfferVideo video = offerVideoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer video not found with id: " + id));

        // Try to delete the file from disk
        try {
            String relativePath = video.getVideoUrl();
            if (relativePath != null && relativePath.startsWith(serverUrl)) {
                relativePath = relativePath.substring(serverUrl.length());
            }
            // Remove leading slash if present
            if (relativePath != null && relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            Path filePath = Paths.get(relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted offer video file from disk: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete offer video file from disk: {}", e.getMessage());
        }

        offerVideoRepository.delete(video);
        log.info("Offer video deleted from database: id={}", id);
    }

    // ──────────────────── Private helpers ────────────────────

    private String saveVideoLocally(MultipartFile file) {
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

            // Return absolute public path (served by StaticResourceConfig)
            String publicPath = serverUrl + "/" + UPLOAD_DIR + "/" + uniqueName;
            log.info("Saved offer video to disk: {}", publicPath);
            return publicPath;
        } catch (IOException e) {
            log.error("Failed to save offer video to disk: {}", e.getMessage());
            throw new RuntimeException("Failed to save offer video file. Please try again.", e);
        }
    }

    private OfferVideoResponseDTO toDTO(OfferVideo entity) {
        String fullVideoUrl = entity.getVideoUrl();

        return OfferVideoResponseDTO.builder()
                .id(entity.getId())
                .videoUrl(fullVideoUrl)
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
