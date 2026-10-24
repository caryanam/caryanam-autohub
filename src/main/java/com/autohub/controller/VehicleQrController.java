package com.autohub.controller;

import com.autohub.dto.ResponseDto;
import com.autohub.dto.VehicleQrResponseDTO;
import com.autohub.service.VehicleQrService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Vehicle QR Code generation.
 * Provides endpoints to get QR code data (base64 JSON) and download QR as PNG.
 */
@RestController
@RequestMapping("/api/vehicle/qr")
@RequiredArgsConstructor
public class VehicleQrController {

    private final VehicleQrService vehicleQrService;

    // ================= GET VEHICLE QR CODE (Base64 JSON) =================

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get vehicle QR code as base64 PNG with vehicle & dealer info")
    public ResponseEntity<ResponseDto<VehicleQrResponseDTO>> getVehicleQrCode(
            @PathVariable Long vehicleId) {

        VehicleQrResponseDTO response = vehicleQrService.getVehicleQrCode(vehicleId);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "Vehicle QR Code generated successfully",
                        response
                )
        );
    }

    // ================= DOWNLOAD VEHICLE QR CODE (PNG file) =================

    @GetMapping("/{vehicleId}/download")
    @Operation(summary = "Download vehicle QR code as PNG image file")
    public ResponseEntity<byte[]> downloadVehicleQrCode(
            @PathVariable Long vehicleId) {

        byte[] qrPngBytes = vehicleQrService.downloadVehicleQrCode(vehicleId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData(
                "attachment",
                "vehicle-" + vehicleId + "-qr.png"
        );
        headers.setContentLength(qrPngBytes.length);

        return new ResponseEntity<>(qrPngBytes, headers, HttpStatus.OK);
    }
}
