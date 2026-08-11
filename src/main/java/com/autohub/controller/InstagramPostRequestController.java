package com.autohub.controller;

import com.autohub.configuration.CustomUserDetails;
import com.autohub.dto.InstagramDealerVehicleStatusDTO;
import com.autohub.dto.InstagramPostRequestBulkRequestDTO;
import com.autohub.dto.InstagramPostRequestBulkResponseDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.InstagramPostRequestService;
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
 * Dealer-facing Instagram Page publishing endpoints. dealerId is always
 * taken from the authenticated JWT principal - never from the request
 * body or a path variable - so a dealer can only ever act on their own
 * vehicles.
 */
@RestController
@RequestMapping("/api/dealer/instagram-post-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEALER')")
public class InstagramPostRequestController {

    private final InstagramPostRequestService instagramPostRequestService;

    @Operation(summary = "Dealer's vehicles with their current Instagram post status")
    @GetMapping("/vehicles")
    public ResponseEntity<ResponseDto<List<InstagramDealerVehicleStatusDTO>>> getVehiclesWithStatus(
            Authentication authentication) {

        Long dealerId = currentDealerId(authentication);

        List<InstagramDealerVehicleStatusDTO> vehicles =
                instagramPostRequestService.getDealerVehicleStatuses(dealerId);

        return ResponseEntity.ok(new ResponseDto<>(200, "Vehicles fetched successfully", vehicles));
    }

    @Operation(summary = "Request up to 10 vehicles be published to the Instagram Page")
    @PostMapping("/bulk")
    public ResponseEntity<ResponseDto<InstagramPostRequestBulkResponseDTO>> requestBulkInstagramPost(
            @Valid @RequestBody InstagramPostRequestBulkRequestDTO request,
            Authentication authentication) {

        Long dealerId = currentDealerId(authentication);

        InstagramPostRequestBulkResponseDTO response =
                instagramPostRequestService.requestBulkInstagramPost(dealerId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(201, "Instagram post request submitted", response));
    }

    private Long currentDealerId(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return user.getId();
    }
}
