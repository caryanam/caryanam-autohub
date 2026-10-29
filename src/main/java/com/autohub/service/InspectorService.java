package com.autohub.service;

import com.autohub.dto.*;
import com.autohub.enums.InspectionStatus;

import java.util.List;

public interface InspectorService {

    // Admin Inspector CRUD
    InspectorResponseDTO createInspector(InspectorRequestDTO request, Long adminId);

    List<InspectorResponseDTO> getAllInspectors();

    InspectorResponseDTO getInspectorById(Long id);

    InspectorResponseDTO updateInspector(Long id, InspectorRequestDTO request);

    void deleteInspector(Long id);

    void toggleInspectorStatus(Long id, boolean active);

    // Inspector Portal Operations
    InspectorResponseDTO getInspectorProfile(Long inspectorId);

    List<VehicleInspectionRequestResponseDTO> getPendingRequestsForInspector(Long inspectorId);

    List<VehicleInspectionRequestResponseDTO> getCompletedRequestsForInspector(Long inspectorId);

    VehicleInspectionRequestResponseDTO getInspectionRequestById(Long requestId);

    VehicleInspectionRequestResponseDTO verifyVehicle(Long requestId, Long inspectorId, InspectionDecisionDTO decision);

    VehicleInspectionRequestResponseDTO rejectVehicle(Long requestId, Long inspectorId, InspectionDecisionDTO decision);

    // Admin Oversight & Reports
    List<VehicleInspectionRequestResponseDTO> getAllInspectionReports(
            InspectionStatus status,
            String area,
            Long dealerId,
            Long inspectorId
    );

    InspectionStatsDTO getInspectionStats();

    VehicleInspectionRequestResponseDTO assignInspector(Long requestId, Long inspectorId);

    // Helper hook when dealer requests certification
    void createOrUpdateInspectionRequestForVehicle(Long vehicleId, Long dealerId);
}

