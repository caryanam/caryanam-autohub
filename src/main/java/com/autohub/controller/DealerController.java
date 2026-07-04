package com.autohub.controller;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.*;
import com.autohub.service.DealerService;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/dealer")
@RequiredArgsConstructor
public class DealerController {

    private final DealerService dealerService;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    // ================= REGISTER DEALER =================

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Dealer Registration API")
    public ResponseEntity<ResponseDto<DealerResponseDTO>> registerDealer(@Valid @RequestPart("dealer") String dealerRequest,
                                                                         @RequestParam(value = "dealerLogo", required = false)
                                                                         MultipartFile dealerLogo,
                                                                         @RequestParam(value = "showroomImage", required = false)
                                                                         MultipartFile showroomImage) throws Exception {

        DealerRegisterDTO dto =objectMapper.readValue(dealerRequest, DealerRegisterDTO.class);

        DealerResponseDTO dealerResponseDTO = dealerService.registerDealer(dto, dealerLogo, showroomImage);

        return new ResponseEntity<>(new ResponseDto(200,"Dealer Registration Successfully",dealerResponseDTO),HttpStatus.OK);
    }

    // ================= UPDATE DEALER PROFILE =================

    @PutMapping("/update-profile/{dealerId}")
    @Operation(summary = "Update Dealer Profile API")
    public ResponseEntity<ResponseDto<DealerProfileResponseDTO>> updateDealerProfile(@PathVariable Long dealerId,@Valid @RequestBody UpdateDealerProfileRequestDTO request,@RequestHeader("Authorization") String authHeader) throws AccessDeniedException {

        validateDealerAccess(authHeader, dealerId);

        DealerProfileResponseDTO dealerResponseDTO = dealerService.updateDealerProfile(dealerId, request);

        return new ResponseEntity<>(new ResponseDto<>(200,"Dealer Profile Updated Successfully",dealerResponseDTO),HttpStatus.OK);
    }

    // ========== GET DEALER  BY ID ================
    @GetMapping("/dealer-profile/{dealerId}")
    @Operation(summary = "Get Dealer Profile By Id API ")
    public ResponseEntity<ResponseDto<DealerResponseDTO>>   getDealerById(@RequestHeader("Authorization") String authHeader,@PathVariable Long dealerId) throws AccessDeniedException {

        validateDealerAccess(authHeader, dealerId);

        DealerResponseDTO dealerProfile = dealerService.getDealerProfile(dealerId);

        return new ResponseEntity<>(new ResponseDto<>(200,"Dealer Profile Fetch Successfully",dealerProfile),HttpStatus.OK);
    }


   // ================= DEALER DASHBOARD =================

    @GetMapping("/dashboard/{dealerId}")
    @Operation(summary = "Dealer Dashboard API Total Vehicles, Featured Vehicles, Total Leads, Vehicle Views")
    public ResponseEntity<DashboardResponseDTO>   getDashboard(@PathVariable Long dealerId,@RequestHeader("Authorization") String authHeader) throws AccessDeniedException {
        validateDealerAccess(authHeader, dealerId);
        return ResponseEntity.ok( dealerService.getDashboard(dealerId));
    }


    // ================= GET AVAILABLE SUBSCRIPTION PLAN =================

    @GetMapping("/subscription/plans")
    @Operation(summary = "Get Available Subscription Plans API ( BASIC, STANDARD, PREMIUM) ")
    public ResponseEntity<ResponseDto<List<SubscriptionPlanDTO>>> getPlans() {

        return ResponseEntity.ok(new ResponseDto<>(200,"Subscription Plans Fetched Successfully",
                dealerService.getAllSubscriptionsPlans()
                )
        );
    }

    // ================= GET CURRENT SUBSCRIPTION PLAN =================

    @GetMapping("/current-plan/{dealerId}")
    @Operation(summary = "Get Current Dealer Active Subscription Plan API")
    public ResponseEntity<ResponseDto<DealerCurrentSubscriptionPlanDTO>> getDealerCurrentSubscription(@RequestHeader("Authorization") String authHeader,@PathVariable Long dealerId) throws AccessDeniedException {

        validateDealerAccess(authHeader, dealerId);

        DealerCurrentSubscriptionPlanDTO dealerSubscriptionPlan =dealerService.getDealerCurrentSubscriptionPlan(dealerId);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "Subscription Details Fetched Successfully",
                        dealerSubscriptionPlan
                )
        );
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