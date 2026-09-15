package com.autohub.service;

import com.autohub.dto.OfferVideoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OfferVideoService {

    /**
     * Upload a new offer video. Automatically enables it and disables all others.
     */
    OfferVideoResponseDTO uploadOfferVideo(MultipartFile video);

    /**
     * Get the uploaded offer video (admin view).
     */
    OfferVideoResponseDTO getAdminOfferVideo();

    /**
     * Get the currently active offer video for the public homepage.
     * Returns null if none is available.
     */
    OfferVideoResponseDTO getActiveOfferVideo();
}
