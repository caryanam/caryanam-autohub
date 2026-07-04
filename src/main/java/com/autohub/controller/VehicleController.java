package com.autohub.controller;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.ResponseDto;
import com.autohub.dto.VehicleRequestDTO;
import com.autohub.dto.VehicleResponseDTO;
import com.autohub.dto.VehicleStatusRequestDTO;
import com.autohub.enums.VehicleStatus;
import com.autohub.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    private final JwtUtil jwtUtil;

    // ================= ADD VEHICLE INFO=================

    @PostMapping(value = "/add/{dealerId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add new vehicle by dealer after purchased subscription plan API ")
    public ResponseEntity<ResponseDto> addVehicle(
            @RequestHeader("Authorization") String authHeader,
           @Valid @RequestPart("vehicle")
            String vehicleJson,
            @RequestPart(value = "images",required = false)
            List<MultipartFile> images,
            @RequestPart(value = "videos",required = false)
            List<MultipartFile> videos, @PathVariable Long dealerId)throws IOException {

        validateDealerAccess(authHeader, dealerId);

        ObjectMapper mapper = new ObjectMapper();

        VehicleRequestDTO vehicleRequestDTO =mapper.readValue(vehicleJson,VehicleRequestDTO.class);

        VehicleResponseDTO response =vehicleService.addVehicleWithData(vehicleRequestDTO,images,videos,dealerId);

        return new ResponseEntity<>( new ResponseDto<>(201,"Vehicle Added Successfully",response),HttpStatus.CREATED);
    }


    // ================= UPDATE VEHICLE INFO=================

    @PutMapping("/update/{vehicleId}")
    @Operation(summary = "Update vehicle info by dealer API")
    public ResponseEntity<ResponseDto<VehicleResponseDTO>> updateVehicle(@PathVariable("vehicleId") Long id,
                                                                         @RequestBody VehicleRequestDTO request,@RequestHeader("Authorization") String authHeader
                                                                         ) throws java.nio.file.AccessDeniedException {
        Long loggedInDealerId =
                jwtUtil.extractId(authHeader.substring(7));

        VehicleResponseDTO response = vehicleService.updateVehicle(id, request,loggedInDealerId);

        return ResponseEntity.ok(new ResponseDto<>(200,"Vehicle Updated Successfully",response));

    }

    // ================= UPDATE VEHICLE STATUS =================

    @PatchMapping("/status/{vehicleId}")
    @Operation(summary = "Update vehicle status ( FEATURED, ACTIVE, INACTIVE ) by dealer API")
    public ResponseEntity<ResponseDto<VehicleStatus>> updateVehicleStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("vehicleId") Long id,
            @RequestBody VehicleStatusRequestDTO request) throws AccessDeniedException {

        Long loggedInDealerId =
                jwtUtil.extractId(authHeader.substring(7));

        VehicleResponseDTO response = vehicleService.updateVehicleStatus(id,request,loggedInDealerId);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "Vehicle Status Updated Successfully",
                        response.getVehicleStatus()
                ));
    }


    // ================= DELETE VEHICLE =================

    @DeleteMapping("/delete/{vehicleId}")
    @Operation(summary = "Delete vehicle by dealer API")
    public ResponseEntity<ResponseDto> deleteVehicle(@PathVariable Long vehicleId,@RequestHeader("Authorization") String authHeader) throws AccessDeniedException {
        Long loggedInDealerId =
                jwtUtil.extractId(authHeader.substring(7));

        vehicleService.deleteVehicle(vehicleId,loggedInDealerId);
        return new ResponseEntity<>(new ResponseDto<>(201,"Vehicle Delete Successfully",null),HttpStatus.OK);
    }



    // ================= GET ALL VEHICLE BY DEALER ID=================
    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Get all vehicle by dealer id API")
    public ResponseEntity<ResponseDto<List<VehicleResponseDTO>>> getAllVehicleByDealerId(
            @PathVariable Long dealerId,
            @RequestHeader("Authorization") String authHeader) throws AccessDeniedException {

        validateDealerAccess(authHeader, dealerId);

        List<VehicleResponseDTO> response =
                vehicleService.getAllVehicleByDealerId(dealerId);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "All Vehicles By Dealer Id fetched successfully",
                        response));
    }


    // ================= GET VEHICLE BY VEHICLE ID=================

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get vehicle by vehicle id API")
    public ResponseEntity<ResponseDto<VehicleResponseDTO>> getVehicleById(@RequestHeader("Authorization") String authHeader,
            @PathVariable Long vehicleId) throws AccessDeniedException {

        Long loggedInDealerId =
                jwtUtil.extractId(authHeader.substring(7));

        VehicleResponseDTO response =
                vehicleService.getVehicleById(vehicleId,loggedInDealerId);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "Vehicle By Vehicle Id fetched successfully",
                        response));
    }


    // ================= GET ALL ACTIVE AND FEATURES AND NON-PREMIUM VEHICLE =================
    @GetMapping("/non-premium/all-vehicle")
    public ResponseEntity<ResponseDto<List<VehicleResponseDTO>>> getAllNonPremiumVehicle(
            @RequestParam(required = false) Long customerId
    ) {

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "All Non Premium Vehicle fetched successfully",
                        vehicleService.getAllNonPremiumVehicle(customerId))
        );
    }

    // ================= GET ALL ACTIVE AND FEATURES AND PREMIUM VEHICLE =================
    @GetMapping("/premium/all-vehicle")
    public ResponseEntity<ResponseDto<List<VehicleResponseDTO>>> getAllPremiumVehicle(
            @RequestParam(required = false) Long customerId
    ) {

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "All Non Premium Vehicle fetched successfully",
                        vehicleService.getAllPremiumVehicle(customerId))
        );
    }

    // ================= GET ALL FEATURED ONLY 10 VEHICLE =================
    @GetMapping("/featured")
    public ResponseEntity<List<VehicleResponseDTO>> getLatestFeaturedVehicles(@RequestParam(required = false) Long customerId) {

        return ResponseEntity.ok(vehicleService.getLatestFeaturedVehicles(customerId)
        );
    }
    // ================= GET ALL LATEST ADDED ONLY 10 VEHICLE =================
    @GetMapping("/latest-vehicles")
    public ResponseEntity<List<VehicleResponseDTO>> getLatestVehicles(@RequestParam(required = false) Long customerId) {

        return ResponseEntity.ok(
                vehicleService.getLatestVehicles(customerId)
        );
    }

    private void validateDealerAccess(
            String authHeader,
            Long dealerId) throws AccessDeniedException {

        Long loggedInDealerId =
                jwtUtil.extractId(authHeader.substring(7));

        if (!loggedInDealerId.equals(dealerId)) {
            throw new AccessDeniedException(
                    "You are not authorized to perform this action");
        }
    }

}
