package com.autohub.controller;


import com.autohub.dto.DealerOfferResponseDTO;
import com.autohub.dto.offer.DealerOfferRequestDTO;
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
     */
    @PostMapping(value = "/send-dealer-offer",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DealerOfferResponseDTO> sendDealerOffer(
            @RequestPart("offerImage") MultipartFile offerImage,
            @RequestParam("offerTitle") String offerTitle,
            @RequestParam("dealerGreetingName") String dealerGreetingName,
            @RequestParam("offerDetails") String offerDetails,
            @RequestParam("benefits") String benefits,
            @RequestParam("contactInfo") String contactInfo) {

        DealerOfferRequestDTO requestDTO = new DealerOfferRequestDTO();
        requestDTO.setOfferTitle(offerTitle);
        requestDTO.setDealerGreetingName(dealerGreetingName);
        requestDTO.setOfferDetails(offerDetails);
        requestDTO.setBenefits(benefits);
        requestDTO.setContactInfo(contactInfo);

        Long adminId = 1L; // replace with your real admin ID extraction

        DealerOfferResponseDTO response =
                dealerOfferService.sendOfferToAllDealers(requestDTO, offerImage, adminId);

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