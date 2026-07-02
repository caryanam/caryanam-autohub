package com.autohub.service;

import com.autohub.dto.MonthlyViewDTO;
import com.autohub.dto.VehicleResponseDTO;

import java.util.List;

public interface VehicleViewService {

    List<MonthlyViewDTO> getMonthlyViews(Long dealerId);

    void saveView(Long vehicleId);

    VehicleResponseDTO getVehicleById(Long vehicleId);
}