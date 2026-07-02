package com.autohub.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface ReportService {

    ResponseEntity<Resource> dealerActivityReport();

    ResponseEntity<Resource> leadConversionReport();

    ResponseEntity<Resource> revenueByPlanReport();

    ResponseEntity<Resource> topCitiesReport();

    ResponseEntity<Resource> vehicleInventoryReport();
}
