package com.autohub.controller;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.MonthlyLeadAnalyticsDTO;
import com.autohub.dto.MonthlyViewDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.CustomerLeadService;
import com.autohub.service.VehicleViewService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final CustomerLeadService leadService;
    private final VehicleViewService vehicleViewService;
    private final JwtUtil jwtUtil;

    // ================= Month wise Vehicle View on vehicle - Analytics For Dealer =================

    @GetMapping("/vehicle-view/{dealerId}")
    @Operation(summary = "To See Total Month wise Views on Vehicle API for Dealer ")
    public ResponseEntity<List<MonthlyViewDTO>> getViewsAnalytics(
            @PathVariable Long dealerId,
            @RequestHeader("Authorization") String authHeader) throws AccessDeniedException {

        validateDealerAccess(authHeader, dealerId);

        return ResponseEntity.ok(vehicleViewService.getMonthlyViews(dealerId)
        );
    }

    // ================= Month wise Leads Leads on Vehicle - Analytics For Dealer  =================

    @GetMapping("/vehicle-lead/{dealerId}")
    @Operation(summary = "To See Total Month wise Leads on Vehicle API for Dealer ")
    public ResponseEntity<ResponseDto<List<MonthlyLeadAnalyticsDTO>>> getLeadAnalytics(@PathVariable Long dealerId,
    @RequestHeader("Authorization") String authHeader) throws AccessDeniedException {

        validateDealerAccess(authHeader, dealerId);

        List<MonthlyLeadAnalyticsDTO> leadAnalytics = leadService.getMonthlyLead(dealerId);

        return new ResponseEntity<>(new ResponseDto(200,"Lead Monthly Analytics Fetch Successfully",leadAnalytics), HttpStatus.OK);
    }


    private Long validateDealerAccess(
            String authHeader,
            Long dealerId) throws AccessDeniedException {

        String token = authHeader.substring(7);

        Long loggedInDealerId =
                jwtUtil.extractId(token);

        if (!loggedInDealerId.equals(dealerId)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this dealer data"
            );
        }

        return loggedInDealerId;
    }

}
