package com.autohub.controller;


import com.autohub.dto.DealerOfferResponseDTO;
import com.autohub.dto.offer.DealerOfferRequestDTO;
import com.autohub.enums.OfferTemplateType;
import com.autohub.service.DealerOfferService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
@Slf4j
public class DealerOfferController {

    private final DealerOfferService dealerOfferService;

    public DealerOfferController(DealerOfferService dealerOfferService) {
        this.dealerOfferService = dealerOfferService;
    }

    /**
     * Send a new offer broadcast to all active dealers.
     * POST /api/admin/offers/send-dealer-offer
     *
     * Admin selects templateType:
     *   IMAGE (default) → sends caryanam_dealer_offers (image + text)
     *   VIDEO           → sends caryanam_dealer_offers_video (video + text)
     *
     * For IMAGE: offerImage is required, offerVideo is ignored
     * For VIDEO: offerVideo is required, offerImage is ignored
     */
    @PostMapping(value = "/send-dealer-offer",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealerOfferResponseDTO> sendDealerOffer(
            @RequestParam(value = "offerImage", required = false) MultipartFile offerImage,
            @RequestParam(value = "offerVideo", required = false) MultipartFile offerVideo,
            @RequestParam("offerTitle") String offerTitle,
            @RequestParam("dealerGreetingName") String dealerGreetingName,
            @RequestParam("offerDetails") String offerDetails,
            @RequestParam("benefits") String benefits,
            @RequestParam("contactInfo") String contactInfo,
            @RequestParam(value = "templateType", defaultValue = "IMAGE") String templateType) {

        // Parse template type — default to IMAGE for backward compatibility
        OfferTemplateType selectedTemplate;
        try {
            selectedTemplate = OfferTemplateType.valueOf(templateType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    DealerOfferResponseDTO.builder()
                            .status("FAILED")
                            .message("Invalid templateType: '" + templateType
                                    + "'. Must be IMAGE or VIDEO.")
                            .build());
        }

        // Validate: correct media file must be provided for selected template
        if (selectedTemplate == OfferTemplateType.IMAGE && (offerImage == null || offerImage.isEmpty())) {
            return ResponseEntity.badRequest().body(
                    DealerOfferResponseDTO.builder()
                            .status("FAILED")
                            .message("offerImage is required when templateType is IMAGE.")
                            .build());
        }
        if (selectedTemplate == OfferTemplateType.VIDEO && (offerVideo == null || offerVideo.isEmpty())) {
            return ResponseEntity.badRequest().body(
                    DealerOfferResponseDTO.builder()
                            .status("FAILED")
                            .message("offerVideo is required when templateType is VIDEO.")
                            .build());
        }

        DealerOfferRequestDTO requestDTO = new DealerOfferRequestDTO();
        requestDTO.setOfferTitle(offerTitle);
        requestDTO.setDealerGreetingName(dealerGreetingName);
        requestDTO.setOfferDetails(offerDetails);
        requestDTO.setBenefits(benefits);
        requestDTO.setContactInfo(contactInfo);
        requestDTO.setTemplateType(selectedTemplate);

        Long adminId = 1L; // replace with your real admin ID extraction

        // Pass the correct media file based on template type
        MultipartFile mediaFile = (selectedTemplate == OfferTemplateType.VIDEO) ? offerVideo : offerImage;

        DealerOfferResponseDTO response =
                dealerOfferService.sendOfferToAllDealers(requestDTO, mediaFile, adminId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all past offer broadcasts with per-dealer delivery status.
     * GET /api/admin/offers/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DealerOfferResponseDTO>> getAllOffers() {
        return ResponseEntity.ok(dealerOfferService.getAllOffers());
    }

    /**
     * Get a specific offer broadcast with full dealer delivery breakdown.
     * GET /api/admin/offers/{offerId}
     */
    @GetMapping("/{offerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealerOfferResponseDTO> getOfferById(
            @PathVariable Long offerId) {
        return ResponseEntity.ok(dealerOfferService.getOfferById(offerId));
    }
}