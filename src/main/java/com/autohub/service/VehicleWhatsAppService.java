package com.autohub.service;


import com.autohub.dto.ShareVehicleRequestDTO;
import com.autohub.dto.VehicleShareResponseDTO;

import java.util.List;

public interface VehicleWhatsAppService {

    // Dealer shares a vehicle to their own WhatsApp (backward compatibility)
    VehicleShareResponseDTO shareVehicleOnWhatsApp(Long vehicleId, Long dealerId);

    // Dealer shares a vehicle to self, customer, or both
    List<VehicleShareResponseDTO> shareVehicleOnWhatsApp(Long vehicleId, ShareVehicleRequestDTO request);

    // Dealer views their own share history
    List<VehicleShareResponseDTO> getDealerShareHistory(Long dealerId);

    // How many times a specific vehicle was shared
    long getVehicleShareCount(Long vehicleId);
}