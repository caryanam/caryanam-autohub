package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for the Vehicle QR Code feature.
 * Contains vehicle summary, dealer contact details, and the QR code as a base64 PNG.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleQrResponseDTO {

    // ── Vehicle info ──
    private Long vehicleId;
    private String brand;
    private String model;
    private String variant;
    private Double askingPrice;
    private String fuelType;
    private String city;
    private Integer registrationYear;
    private Long kilometerDriven;

    // ── Dealer contact fields ──
    private String dealerBusinessName;      // e.g. "Sharma Motors"
    private String dealerName;              // e.g. "Rajesh Sharma" (owner name)
    private String dealerContactNumber;     // e.g. "9876543210"
    private String dealerWhatsappNumber;    // e.g. "9876543210"

    // ── QR output ──
    private String vehiclePageUrl;          // https://caryanam.com/car/42
    private String qrCodeBase64;            // PNG image as Base64-encoded string
}
