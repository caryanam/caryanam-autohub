package com.autohub.controller;

import com.autohub.dto.OfferVideoResponseDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.OfferVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Offer Video", description = "Admin upload and public display of homepage offer videos")
public class OfferVideoController {

    private final OfferVideoService offerVideoService;

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Upload a new offer video. Automatically enables it (disables all others).
     * POST /api/admin/offer-video/upload
     */
    @PostMapping(value = "/api/admin/offer-video/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a new offer video for the homepage")
    public ResponseEntity<ResponseDto<OfferVideoResponseDTO>> uploadOfferVideo(
            @RequestParam("video") MultipartFile video) {

        if (video == null || video.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400, "Video file is required.", null));
        }

        if (!video.getContentType().startsWith("video/")) {
            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400, "Only video files (MP4, etc.) are allowed.", null));
        }

        OfferVideoResponseDTO response = offerVideoService.uploadOfferVideo(video);
        return ResponseEntity.ok(new ResponseDto<>(200, "Offer video uploaded and enabled successfully.", response));
    }

    /**
     * Get the uploaded offer video (admin view).
     * GET /api/admin/offer-video
     */
    @GetMapping("/api/admin/offer-video")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get the uploaded offer video for admin management")
    public ResponseEntity<ResponseDto<OfferVideoResponseDTO>> getAdminOfferVideo() {
        OfferVideoResponseDTO video = offerVideoService.getAdminOfferVideo();
        return ResponseEntity.ok(new ResponseDto<>(200, "Offer video fetched successfully.", video));
    }



    // ==================== PUBLIC ENDPOINT ====================

    /**
     * Get the currently active (enabled) offer video for the public homepage.
     * GET /api/offer-video/active
     * No authentication required — accessible to all website visitors.
     */
    @GetMapping("/api/offer-video/active")
    @Operation(summary = "Get the active offer video for the homepage (public)")
    public ResponseEntity<ResponseDto<OfferVideoResponseDTO>> getActiveOfferVideo() {
        OfferVideoResponseDTO active = offerVideoService.getActiveOfferVideo();
        if (active == null) {
            return ResponseEntity.ok(new ResponseDto<>(200, "No active offer video.", null));
        }
        return ResponseEntity.ok(new ResponseDto<>(200, "Active offer video fetched.", active));
    }
}
