package com.autohub.dto;

/**
 * Request body for vehicle WhatsApp share.
 * Currently empty since everything is derived from vehicleId + dealer context,
 * but kept as a DTO for future extensibility (e.g. adding a custom note).
 * Frontend just sends an empty JSON body: {}
 */
public class VehicleShareRequestDTO {
    // Reserved for future: custom message, override recipient number, etc.
}