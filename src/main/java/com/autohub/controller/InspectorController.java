package com.autohub.controller;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.InspectionDecisionDTO;
import com.autohub.dto.InspectorResponseDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.dto.VehicleInspectionRequestResponseDTO;
import com.autohub.enums.InspectionStatus;
import com.autohub.service.InspectorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inspector")
@RequiredArgsConstructor
public class InspectorController {

    private final InspectorService inspectorService;
    private final JwtUtil jwtUtil;

    // ================= INSPECTOR PROFILE =================
    @GetMapping("/profile")
    @Operation(summary = "Get profile and assigned area of currently logged-in inspector")
    public ResponseEntity<ResponseDto<InspectorResponseDTO>> getProfile(HttpServletRequest request) {
        Long inspectorId = extractInspectorId(request);
        InspectorResponseDTO response = inspectorService.getInspectorProfile(inspectorId);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspector profile fetched successfully", response));
    }

    // ================= PENDING REQUESTS IN ASSIGNED AREA =================
    @GetMapping("/requests/pending")
    @Operation(summary = "Get all pending vehicle certification requests matching the inspector's assigned area")
    public ResponseEntity<ResponseDto<List<VehicleInspectionRequestResponseDTO>>> getPendingRequests(
            HttpServletRequest request
    ) {
        Long inspectorId = extractInspectorId(request);
        List<VehicleInspectionRequestResponseDTO> list = inspectorService.getPendingRequestsForInspector(inspectorId);
        return ResponseEntity.ok(new ResponseDto<>(200, "Pending inspection requests fetched successfully", list));
    }

    // ================= COMPLETED INSPECTION HISTORY =================
    @GetMapping("/requests/history")
    @Operation(summary = "Get historical inspections completed (verified/rejected) by this inspector")
    public ResponseEntity<ResponseDto<List<VehicleInspectionRequestResponseDTO>>> getInspectionHistory(
            HttpServletRequest request
    ) {
        Long inspectorId = extractInspectorId(request);
        List<VehicleInspectionRequestResponseDTO> list = inspectorService.getCompletedRequestsForInspector(inspectorId);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspection history fetched successfully", list));
    }

    // ================= GET SINGLE INSPECTION REQUEST DETAILS =================
    @GetMapping("/request/{requestId}")
    @Operation(summary = "Get vehicle and dealer details for a specific inspection request")
    public ResponseEntity<ResponseDto<VehicleInspectionRequestResponseDTO>> getRequestById(
            @PathVariable Long requestId
    ) {
        VehicleInspectionRequestResponseDTO response = inspectorService.getInspectionRequestById(requestId);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspection request details fetched successfully", response));
    }

    // ================= COMPLETE VERIFICATION =================
    @PostMapping("/verify/{requestId}")
    @Operation(summary = "Mark vehicle certification as COMPLETE VERIFY after physical inspection")
    public ResponseEntity<ResponseDto<VehicleInspectionRequestResponseDTO>> verifyVehicle(
            @PathVariable Long requestId,
            @RequestBody(required = false) InspectionDecisionDTO decision,
            HttpServletRequest request
    ) {
        Long inspectorId = extractInspectorId(request);
        if (decision == null) {
            decision = InspectionDecisionDTO.builder()
                    .status(InspectionStatus.VERIFIED)
                    .inspectionNotes("Vehicle physically verified and certified.")
                    .build();
        } else {
            decision.setStatus(InspectionStatus.VERIFIED);
        }

        VehicleInspectionRequestResponseDTO response = inspectorService.verifyVehicle(
                requestId, inspectorId, decision
        );
        return ResponseEntity.ok(new ResponseDto<>(200, "Vehicle certified and verified successfully", response));
    }

    // ================= REJECT INSPECTION =================
    @PostMapping("/reject/{requestId}")
    @Operation(summary = "Reject vehicle certification if vehicle fails physical inspection")
    public ResponseEntity<ResponseDto<VehicleInspectionRequestResponseDTO>> rejectVehicle(
            @PathVariable Long requestId,
            @RequestBody(required = false) InspectionDecisionDTO decision,
            HttpServletRequest request
    ) {
        Long inspectorId = extractInspectorId(request);
        if (decision == null) {
            decision = InspectionDecisionDTO.builder()
                    .status(InspectionStatus.REJECTED)
                    .inspectionNotes("Vehicle failed physical inspection.")
                    .build();
        } else {
            decision.setStatus(InspectionStatus.REJECTED);
        }

        VehicleInspectionRequestResponseDTO response = inspectorService.rejectVehicle(
                requestId, inspectorId, decision
        );
        return ResponseEntity.ok(new ResponseDto<>(200, "Vehicle inspection marked as rejected", response));
    }

    private Long extractInspectorId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractId(token);
        }
        throw new RuntimeException("Authorization token missing or invalid");
    }
}

