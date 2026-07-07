package com.autohub.controller;

import com.autohub.service.PincodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pincode")
@RequiredArgsConstructor
public class PincodeController {

    private final PincodeService service;

    @PostMapping("/import")
    public ResponseEntity<?> importExcel(
            @RequestParam MultipartFile file)
            throws Exception {

        service.importExcel(file);

        return ResponseEntity.ok(
                "Imported Successfully");
    }

    @GetMapping("/cities")
    public ResponseEntity<?> getCities() {

        return ResponseEntity.ok(
                service.getCities());
    }

    @GetMapping("/areas")
    public ResponseEntity<?> getAreas(
            @RequestParam String city) {

        return ResponseEntity.ok(
                service.getAreas(city));
    }

    @GetMapping("/area")
    public ResponseEntity<?> getByArea(
            @RequestParam String area) {

        return ResponseEntity.ok(
                service.getByArea(area));
    }

    @GetMapping("/pincode")
    public ResponseEntity<?> getByPincode(
            @RequestParam String pincode) {

        return ResponseEntity.ok(
                service.getByPincode(pincode));
    }

    @GetMapping("/all-areas")
    public ResponseEntity<List<String>> getAllAreas() {

        return ResponseEntity.ok(
                service.getAllAreas()
        );
    }
}