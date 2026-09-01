package com.autohub.controller;

import com.autohub.dto.SocialVisitLogDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.SocialVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/social-visits")
@RequiredArgsConstructor
public class AdminSocialVisitController {

    private final SocialVisitService visitService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<SocialVisitLogDTO>>> getAllVisits() {
        return ResponseEntity.ok(new ResponseDto<>(200, "Fetched successfully", visitService.getAllVisits()));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ResponseDto<List<SocialVisitLogDTO>>> getVisitsByVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(new ResponseDto<>(200, "Fetched successfully", visitService.getVisitsByVehicle(vehicleId)));
    }

    @GetMapping("/dealer/{dealerId}")
    public ResponseEntity<ResponseDto<List<SocialVisitLogDTO>>> getVisitsByDealer(@PathVariable Long dealerId) {
        return ResponseEntity.ok(new ResponseDto<>(200, "Fetched successfully", visitService.getVisitsByDealer(dealerId)));
    }
}
