package com.autohub.controller;

import com.autohub.dto.*;
import com.autohub.service.CustomerLeadService;
import com.autohub.service.VehicleViewService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lead")
@RequiredArgsConstructor
public class LeadController {

    private final CustomerLeadService leadService;

    private final VehicleViewService vehicleViewService;

    // =============== ADD NEW CUSTOMER REGISTRATION OR LEAD ON VEHICLE FROM CUSTOMER =====================

    @PostMapping("/generate-lead/{vehicleId}")
    @Operation(summary = "Add new lead and registration customer on vehicle from customer/user API ")
    public ResponseEntity<ResponseDto<CustomerLeadResponseDTO>> createCustomerLead(
            @PathVariable Long vehicleId,
            @RequestBody CustomerLeadRequestDTO requestDTO) {

        CustomerLeadResponseDTO lead = leadService.createLead(vehicleId, requestDTO);


        return ResponseEntity.ok(new ResponseDto<>(200,"New lead created successfully",lead));
    }

    // =============== ADD VIEW ON VEHICLE FROM CUSTOMER =====================

    @GetMapping("/generate-view/{vehicleId}")
    @Operation(summary = "Add new view on vehicle from customer/user API ")
    public ResponseEntity<ResponseDto<VehicleResponseDTO>> getVehicleById(@PathVariable Long vehicleId) {

        vehicleViewService.saveView(vehicleId);

        VehicleResponseDTO vehicleById = vehicleViewService.getVehicleById(vehicleId);

        return ResponseEntity.ok(new ResponseDto<>(200,"New View Added successfully",vehicleById));
    }

    // ================= ALL CUSTOMER OR LEADS OF CUSTOMER FOR DEALER =================

    @GetMapping("/all-leads/{dealerId}")
    @Operation(summary = "Get all leads of customer/user API ")
    public ResponseEntity<ResponseDto<List<CustomerLeadResponseDTO>>> getCustomerLeads(
            @PathVariable Long dealerId) {

        List<CustomerLeadResponseDTO> response = leadService.getDealerLeads(dealerId);

        return ResponseEntity.ok(new ResponseDto<>(200,"Customer All Leads Fetched Successfully",response)
        );
    }

    // ================= UPDATE REGISTRATION OR LEADS STATUS=================

    @PutMapping("/lead-status/{leadId}")
    @Operation(summary = "Update lead status API (NEW, CONTACTED, CONVERTED)  ")
    public ResponseEntity<ResponseDto<CustomerLeadResponseDTO>> updateLeadStatus(
            @PathVariable Long leadId,
            @RequestBody CustomerLeadStatusRequestDTO requestDTO) {
        CustomerLeadResponseDTO response = leadService.updateLeadStatus(leadId, requestDTO);

        return ResponseEntity.ok(new ResponseDto<>(200,"Lead status successfully",response));
    }

    // =============== ADD CUSTOMER DASHBOARD =====================

    @GetMapping("/customer-dashboard")
    @Operation(summary = "Customer Dashboard")
    public ResponseEntity<?> getCustomerDashboard() {

        return ResponseEntity.ok("Customer Dashboard Fetch Successfully");
    }

}
