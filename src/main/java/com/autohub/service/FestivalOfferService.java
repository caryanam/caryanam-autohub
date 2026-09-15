package com.autohub.service;

import com.autohub.dto.FestivalOfferResponseDTO;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

public interface FestivalOfferService {

    /**
     * Upload a new festival offer. Automatically enables it and disables/deletes others.
     */
    FestivalOfferResponseDTO uploadFestivalOffer(MultipartFile file, LocalDateTime expiresAt);

    /**
     * Get the uploaded festival offer (admin view).
     */
    FestivalOfferResponseDTO getAdminFestivalOffer();

    /**
     * Get the currently active festival offer for the public homepage.
     * Returns null if none is available or if expired.
     */
    FestivalOfferResponseDTO getActiveFestivalOffer();

    /**
     * Disable a festival offer manually.
     */
    FestivalOfferResponseDTO disableFestivalOffer(Long id);
}
