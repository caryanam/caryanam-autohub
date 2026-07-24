package com.autohub.service;


import com.autohub.dto.VehicleShareResponseDTO;

import java.util.List;

public interface VehicleWhatsAppService {

    // Dealer shares a vehicle to their own WhatsApp
    VehicleShareResponseDTO shareVehicleOnWhatsApp(Long vehicleId, Long dealerId);

    // Dealer views their own share history
    List<VehicleShareResponseDTO> getDealerShareHistory(Long dealerId);

    // How many times a specific vehicle was shared
    long getVehicleShareCount(Long vehicleId);
}