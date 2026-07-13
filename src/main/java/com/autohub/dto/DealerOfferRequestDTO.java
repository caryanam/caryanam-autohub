package com.autohub.dto.offer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Received as multipart/form-data alongside the image file.
 * The image itself comes as MultipartFile in the controller.
 */
@Getter
@Setter
public class DealerOfferRequestDTO {

    @NotBlank(message = "Offer title is required")
    private String offerTitle;

    @NotBlank(message = "Dealer greeting name is required")
    private String dealerGreetingName; // {{1}} - used as generic greeting for all dealers
    // e.g. "Valued Partner" or "Dear Dealer"

    @NotBlank(message = "Offer details are required")
    private String offerDetails;       // {{2}}

    @NotBlank(message = "Benefits are required")
    private String benefits;           // {{3}}

    @NotBlank(message = "Contact info is required")
    private String contactInfo;        // {{4}}
}