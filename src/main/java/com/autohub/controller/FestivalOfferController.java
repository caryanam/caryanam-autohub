package com.autohub.controller;

import com.autohub.dto.FestivalOfferResponseDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.FestivalOfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Festival Offer Management", description = "APIs for managing festival offers (image/video)")
public class FestivalOfferController {

    private final FestivalOfferService festivalOfferService;

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Upload a new festival offer.
     * POST /api/admin/festival-offer/upload
     */
    @PostMapping(value = "/api/admin/festival-offer/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a new festival offer (image or video) with a duration")
    public ResponseEntity<ResponseDto<FestivalOfferResponseDTO>> uploadFestivalOffer(
            @RequestParam("file") MultipartFile file,
            @RequestParam("durationHours") Integer durationHours) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400, "Media file is required.", null));
        }

        if (durationHours == null || durationHours <= 0) {
            return ResponseEntity.badRequest().body(
                    new ResponseDto<>(400, "Duration must be greater than 0.", null));
        }

        FestivalOfferResponseDTO response = festivalOfferService.uploadFestivalOffer(file, durationHours);
        return ResponseEntity.ok(new ResponseDto<>(200, "Festival offer uploaded successfully.", response));
    }

    /**
     * Get the uploaded festival offer (admin view).
     * GET /api/admin/festival-offer
     */
    @GetMapping("/api/admin/festival-offer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get the uploaded festival offer for admin management")
    public ResponseEntity<ResponseDto<FestivalOfferResponseDTO>> getAdminFestivalOffer() {
        FestivalOfferResponseDTO offer = festivalOfferService.getAdminFestivalOffer();
        return ResponseEntity.ok(new ResponseDto<>(200, "Festival offer fetched successfully.", offer));
    }

    /**
     * Disable a festival offer.
     * PUT /api/admin/festival-offer/{id}/disable
     */
    @PutMapping("/api/admin/festival-offer/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable a festival offer manually")
    public ResponseEntity<ResponseDto<FestivalOfferResponseDTO>> disableFestivalOffer(@PathVariable Long id) {
        FestivalOfferResponseDTO response = festivalOfferService.disableFestivalOffer(id);
        return ResponseEntity.ok(new ResponseDto<>(200, "Festival offer disabled successfully.", response));
    }

    // ==================== PUBLIC ENDPOINT ====================

    /**
     * Get the currently active festival offer for the homepage.
     * GET /api/festival-offer/active
     */
    @GetMapping("/api/festival-offer/active")
    @Operation(summary = "Get the currently active festival offer")
    public ResponseEntity<ResponseDto<FestivalOfferResponseDTO>> getActiveFestivalOffer() {
        FestivalOfferResponseDTO offer = festivalOfferService.getActiveFestivalOffer();
        if (offer == null) {
            return ResponseEntity.ok(new ResponseDto<>(200, "No active festival offer found.", null));
        }
        return ResponseEntity.ok(new ResponseDto<>(200, "Active festival offer fetched successfully.", offer));
    }
}
