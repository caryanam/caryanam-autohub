package com.autohub.controller;

import com.autohub.configuration.CustomUserDetails;
import com.autohub.dto.FacebookDealerVehicleStatusDTO;
import com.autohub.dto.FacebookPostRequestBulkRequestDTO;
import com.autohub.dto.FacebookPostRequestBulkResponseDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.FacebookPostRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dealer-facing Facebook Page publishing endpoints. dealerId is always
 * taken from the authenticated JWT principal - never from the request
 * body or a path variable - so a dealer can only ever act on their own
 * vehicles.
 */
@RestController
@RequestMapping("/api/dealer/facebook-post-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEALER')")
public class FacebookPostRequestController {

    private final FacebookPostRequestService facebookPostRequestService;

    @Operation(summary = "Dealer's vehicles with their current Facebook post status")
    @GetMapping("/vehicles")
    public ResponseEntity<ResponseDto<List<FacebookDealerVehicleStatusDTO>>> getVehiclesWithStatus(
            Authentication authentication) {

        Long dealerId = currentDealerId(authentication);

        List<FacebookDealerVehicleStatusDTO> vehicles =
                facebookPostRequestService.getDealerVehicleStatuses(dealerId);

        return ResponseEntity.ok(new ResponseDto<>(200, "Vehicles fetched successfully", vehicles));
    }

    @Operation(summary = "Request up to 10 vehicles be published to the Facebook Page")
    @PostMapping("/bulk")
    public ResponseEntity<ResponseDto<FacebookPostRequestBulkResponseDTO>> requestBulkFacebookPost(
            @Valid @RequestBody FacebookPostRequestBulkRequestDTO request,
            Authentication authentication) {

        Long dealerId = currentDealerId(authentication);

        FacebookPostRequestBulkResponseDTO response =
                facebookPostRequestService.requestBulkFacebookPost(dealerId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(201, "Facebook post request submitted", response));
    }

    private Long currentDealerId(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return user.getId();
    }
}
