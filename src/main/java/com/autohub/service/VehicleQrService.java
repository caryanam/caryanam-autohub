package com.autohub.service;

import com.autohub.dto.VehicleQrResponseDTO;

/**
 * Service interface for Vehicle QR Code generation.
 */
public interface VehicleQrService {

    /**
     * Generate QR code data for a vehicle (returns JSON-friendly DTO with base64 QR image).
     *
     * @param vehicleId the vehicle ID
     * @return VehicleQrResponseDTO with vehicle info, dealer contacts, and QR PNG as base64
     */
    VehicleQrResponseDTO getVehicleQrCode(Long vehicleId);

    /**
     * Generate QR code as raw PNG bytes for direct file download.
     *
     * @param vehicleId the vehicle ID
     * @return PNG image bytes
     */
    byte[] downloadVehicleQrCode(Long vehicleId);
}
