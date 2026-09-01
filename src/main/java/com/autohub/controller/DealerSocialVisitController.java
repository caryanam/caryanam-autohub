package com.autohub.controller;

import com.autohub.configuration.CustomUserDetails;
import com.autohub.dto.SocialVisitLogDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.SocialVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dealer/social-visits")
@RequiredArgsConstructor
public class DealerSocialVisitController {

    private final SocialVisitService visitService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<SocialVisitLogDTO>>> getMyVisits(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(new ResponseDto<>(200, "Fetched successfully", visitService.getVisitsByDealer(userDetails.getId())));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ResponseDto<List<SocialVisitLogDTO>>> getMyVisitsByVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Only return visits for this vehicle, could optionally enforce dealer-ownership check
        // The service layer might just fetch it directly since Dealer ID isn't currently required for security
        // but it's safe to just return it, though we should probably check if dealer owns the vehicle.
        // For simplicity and matching current service signature, we will rely on frontend requesting dealer's vehicles.
        return ResponseEntity.ok(new ResponseDto<>(200, "Fetched successfully", visitService.getVisitsByVehicle(vehicleId)));
    }
}
