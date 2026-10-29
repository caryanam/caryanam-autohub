package com.autohub.controller;

import com.autohub.configuration.JwtUtil;
import com.autohub.dto.*;
import com.autohub.enums.InspectionStatus;
import com.autohub.service.InspectorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminInspectorController {

    private final InspectorService inspectorService;
    private final JwtUtil jwtUtil;

    // ================= CREATE INSPECTOR (NO DIRECT REGISTRATION) =================
    @PostMapping("/inspectors")
    @Operation(summary = "Admin creates an Inspector account with assigned area")
    public ResponseEntity<ResponseDto<InspectorResponseDTO>> createInspector(
            @Valid @RequestBody InspectorRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        Long adminId = extractUserId(request);
        InspectorResponseDTO response = inspectorService.createInspector(requestDTO, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(201, "Inspector created successfully with assigned area", response));
    }

    // ================= GET ALL INSPECTORS =================
    @GetMapping("/inspectors")
    @Operation(summary = "Get all inspectors with assigned areas and verification stats")
    public ResponseEntity<ResponseDto<List<InspectorResponseDTO>>> getAllInspectors() {
        List<InspectorResponseDTO> list = inspectorService.getAllInspectors();
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspectors fetched successfully", list));
    }

    // ================= GET INSPECTOR BY ID =================
    @GetMapping("/inspectors/{id}")
    @Operation(summary = "Get inspector details by ID")
    public ResponseEntity<ResponseDto<InspectorResponseDTO>> getInspectorById(@PathVariable Long id) {
        InspectorResponseDTO response = inspectorService.getInspectorById(id);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspector fetched successfully", response));
    }

    // ================= UPDATE INSPECTOR =================
    @PutMapping("/inspectors/{id}")
    @Operation(summary = "Update inspector details, assigned area, or password")
    public ResponseEntity<ResponseDto<InspectorResponseDTO>> updateInspector(
            @PathVariable Long id,
            @RequestBody InspectorRequestDTO requestDTO
    ) {
        InspectorResponseDTO response = inspectorService.updateInspector(id, requestDTO);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspector updated successfully", response));
    }

    // ================= DEACTIVATE / DELETE INSPECTOR =================
    @DeleteMapping("/inspectors/{id}")
    @Operation(summary = "Deactivate an inspector")
    public ResponseEntity<ResponseDto<String>> deleteInspector(@PathVariable Long id) {
        inspectorService.deleteInspector(id);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspector deactivated successfully", null));
    }

    // ================= TOGGLE INSPECTOR ACTIVE STATUS =================
    @PatchMapping("/inspectors/{id}/status")
    @Operation(summary = "Toggle inspector active/inactive status")
    public ResponseEntity<ResponseDto<String>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        inspectorService.toggleInspectorStatus(id, active);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspector status updated to " + (active ? "ACTIVE" : "INACTIVE"), null));
    }

    // ================= ALL INSPECTION REQUESTS / REPORTS =================
    @GetMapping("/inspections/all")
    @Operation(summary = "Comprehensive report: All vehicle certification requests with dealer, vehicle, and inspector info")
    public ResponseEntity<ResponseDto<List<VehicleInspectionRequestResponseDTO>>> getAllInspections(
            @RequestParam(required = false) InspectionStatus status,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Long dealerId,
            @RequestParam(required = false) Long inspectorId
    ) {
        List<VehicleInspectionRequestResponseDTO> reports = inspectorService.getAllInspectionReports(
                status, area, dealerId, inspectorId
        );
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspection reports fetched successfully", reports));
    }

    // ================= INSPECTION STATS =================
    @GetMapping("/inspections/stats")
    @Operation(summary = "Get overview KPIs for vehicle inspections and verifications")
    public ResponseEntity<ResponseDto<InspectionStatsDTO>> getInspectionStats() {
        InspectionStatsDTO stats = inspectorService.getInspectionStats();
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspection statistics fetched successfully", stats));
    }

    // ================= ASSIGN / REASSIGN INSPECTOR =================
    @PutMapping("/inspections/{requestId}/assign/{inspectorId}")
    @Operation(summary = "Admin manually assigns or reassigns an inspector to a vehicle inspection request")
    public ResponseEntity<ResponseDto<VehicleInspectionRequestResponseDTO>> assignInspector(
            @PathVariable Long requestId,
            @PathVariable Long inspectorId
    ) {
        VehicleInspectionRequestResponseDTO response = inspectorService.assignInspector(requestId, inspectorId);
        return ResponseEntity.ok(new ResponseDto<>(200, "Inspector assigned to inspection request successfully", response));
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtUtil.extractId(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

