package com.autohub.controller;

import com.autohub.dto.*;
import com.autohub.repository.VehicleRepository;
import com.autohub.service.AdminService;
import com.autohub.service.DealerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DealerService dealerService;

    private final AdminService adminService;

    private final VehicleRepository vehicleRepository;

    // ================= CHANGE DEALER ACCOUNT STATUS :  APPROVED, PENDING =================
    @PutMapping("/dealer-status/{dealerId}")
    @Operation(summary = "Change Dealer Account Status By Admin API : Approve or Pending  ")
    public ResponseEntity<ResponseDto<DealerResponseDTO>> updateDealerStatus(
            @PathVariable Long dealerId,
            @RequestBody DealerAccountStatusRequestDTO requestDTO) {
        DealerResponseDTO response = dealerService.updateDealerAccountStatus(dealerId, requestDTO);

        return ResponseEntity.ok(new ResponseDto<>(200,"Dealer account approved successfully",response));
    }

    // ================= GET ALL HISTORY OF SUBSCRIPTION PLAN PURCHASED BY DEALER =================

    @GetMapping("/subscriptions")
    @Operation(summary = "See All Subscription Plan Purchased By Dealer API for Admin ")
    public ResponseEntity<ResponseDto<List<DealerSubscriptionResponseDTO>>> getSubscriptions() {

        List<DealerSubscriptionResponseDTO> data = dealerService.getSubscriptions();
        return ResponseEntity.ok(new ResponseDto<>(200, "All Purchase Subscriptions  fetched of Dealer Fetch Successfully", data));
    }


    // ================= GET ALL DEALER =================
    @GetMapping("/all-dealers")
    public ResponseEntity<List<DealerResponseDTO>> getAllDealers() {
        return ResponseEntity.ok(adminService.allDealer());
    }

    // ================= TOTAL COUNT OF DEALER =================
    @GetMapping("/dealer/count")
    public ResponseEntity<DealerCountResponseDTO> getTotalDealerCount() {

        return ResponseEntity.ok(
                adminService.getTotalDealerCount()
        );
    }
    // ================= TOTAL COUNT OF PENDING DEALER =================
    @GetMapping("/pending/count")
    public ResponseEntity<PendingDealerCountResponseDTO> getPendingDealerCount() {

        return ResponseEntity.ok( adminService.getPendingDealerCount() );
    }

    // ================= TOTAL VEHICLE =================
    @GetMapping("/all-vehicle")
    @Operation(summary = "Get all vehicle API")
    public ResponseEntity<ResponseDto<List<VehicleResponseDTO>>> getAllVehicle() {

        List<VehicleResponseDTO> response =adminService.getAllVehicle();

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "All Vehicles fetched successfully",
                        response));
    }

    // ================= TOTAL COUNT OF VEHICLE =================
    @GetMapping("/vehicle/count")
    public ResponseEntity<VehicleCountResponseDTO> getTotalVehicleCount() {

        return ResponseEntity.ok(adminService.getTotalVehicleCount());
    }

    // ================= GET ALL LEADS =================
    @GetMapping("/all-leads")
    public ResponseEntity<List<AllCustomerLeadResponseDTO>> getAllCustomerLeads() {

        return ResponseEntity.ok(adminService.getAllCustomerLeads());
    }

    // ================= TOTAL COUNT OF  LEADS =================
    @GetMapping("/customer-lead/count")
    public ResponseEntity<CustomerLeadCountResponseDTO> getTotalCustomerLeadCount() {

        return ResponseEntity.ok(
                adminService.getTotalCustomerLeadCount()
        );
    }

    @GetMapping("/total-revenue/count")
    public ResponseEntity<RevenueCountResponseDTO> getTotalRevenue() {
        return ResponseEntity.ok(adminService.getTotalRevenue());
    }

    // Month-wise Dealer Registrations
    @GetMapping("/monthly-dealer-registrations")
    public ResponseEntity<List<AdminMonthlyDealerAnalyticsDTO>> getMonthlyDealerAnalytics() {

        return ResponseEntity.ok(
                adminService.getMonthlyDealerAnalytics()
        );
    }

    // Month-wise Leads
    @GetMapping("/monthly-leads")
    public ResponseEntity<List<AdminMonthlyLeadAnalyticsDTO>> getMonthlyLead() {

        return ResponseEntity.ok(
                adminService.getMonthlyLead()
        );
    }

    // Month-wise Revenue
    @GetMapping("/monthly-revenue")
    public ResponseEntity<List<AdminMonthlyRevenueDTO>> getRevenueAnalytics() {

        return ResponseEntity.ok(
                adminService.getMonthlyRevenueAnalytics()
        );
    }



}






