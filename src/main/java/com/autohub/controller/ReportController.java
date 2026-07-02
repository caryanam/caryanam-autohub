package com.autohub.controller;

import com.autohub.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dealer-activity")
    public ResponseEntity<Resource> dealerActivityReport() {
        return reportService.dealerActivityReport();
    }

    @GetMapping("/lead-conversion")
    public ResponseEntity<Resource> leadConversionReport() {
        return reportService.leadConversionReport();
    }

    @GetMapping("/revenue-by-plan")
    public ResponseEntity<Resource> revenueByPlanReport() {
        return reportService.revenueByPlanReport();
    }

    @GetMapping("/top-cities")
    public ResponseEntity<Resource> topCitiesReport() {
        return reportService.topCitiesReport();
    }

    @GetMapping("/vehicle-inventory")
    public ResponseEntity<Resource> vehicleInventoryReport() {
        return reportService.vehicleInventoryReport();
    }
}
