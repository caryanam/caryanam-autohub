package com.autohub.repository;

import com.autohub.entity.VehicleInspectionRequest;
import com.autohub.enums.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleInspectionRequestRepository extends JpaRepository<VehicleInspectionRequest, Long> {

    List<VehicleInspectionRequest> findByAreaIgnoreCaseAndStatus(String area, InspectionStatus status);

    List<VehicleInspectionRequest> findByAreaIgnoreCase(String area);

    List<VehicleInspectionRequest> findByInspectorId(Long inspectorId);

    List<VehicleInspectionRequest> findByInspectorIdAndStatus(Long inspectorId, InspectionStatus status);

    List<VehicleInspectionRequest> findByStatus(InspectionStatus status);

    List<VehicleInspectionRequest> findByDealerId(Long dealerId);

    List<VehicleInspectionRequest> findByVehicleId(Long vehicleId);

    Optional<VehicleInspectionRequest> findFirstByVehicleIdOrderByRequestDateDesc(Long vehicleId);

    List<VehicleInspectionRequest> findAllByOrderByRequestDateDesc();

    long countByStatus(InspectionStatus status);

    void deleteByVehicleId(Long vehicleId);

    @Query("""
        SELECT r FROM VehicleInspectionRequest r
        WHERE (LOWER(r.area) = LOWER(:area) OR r.inspector.id = :inspectorId)
        AND r.status = :status
        ORDER BY r.requestDate DESC
    """)
    List<VehicleInspectionRequest> findPendingForInspectorOrArea(
            @Param("area") String area,
            @Param("inspectorId") Long inspectorId,
            @Param("status") InspectionStatus status
    );
}
