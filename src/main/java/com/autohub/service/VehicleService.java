package com.autohub.service;

import com.autohub.dto.VehicleRequestDTO;
import com.autohub.dto.VehicleResponseDTO;
import com.autohub.dto.VehicleStatusRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface VehicleService {

    VehicleResponseDTO addVehicleWithData(VehicleRequestDTO vehicleRequestDTO,List<MultipartFile> images,List<MultipartFile> videos,
            Long dealerId) throws IOException;

    VehicleResponseDTO updateVehicle(Long id, VehicleRequestDTO request);

    VehicleResponseDTO updateVehicleStatus(Long id, VehicleStatusRequestDTO request);

    void deleteVehicle(Long id);

    List<VehicleResponseDTO> getAllVehicleByDealerId(Long dealerId);

    VehicleResponseDTO getVehicleById(Long vehicleId);

    List<VehicleResponseDTO> getLatestFeaturedVehicles(Long customerId);

    List<VehicleResponseDTO> getLatestVehicles(Long customerId);

    List<VehicleResponseDTO> getAllNonPremiumVehicle(Long customerId);

    List<VehicleResponseDTO> getAllPremiumVehicle(Long customerId);


  }


