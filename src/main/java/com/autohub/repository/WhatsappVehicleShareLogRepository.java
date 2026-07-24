package com.autohub.repository;

import com.autohub.entity.WhatsappVehicleShareLog;
import com.autohub.enums.WhatsappMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsappVehicleShareLogRepository
        extends JpaRepository<WhatsappVehicleShareLog, Long> {

    // All share attempts for a specific vehicle
    List<WhatsappVehicleShareLog> findByVehicleIdOrderBySharedAtDesc(Long vehicleId);

    // All shares by a specific dealer
    List<WhatsappVehicleShareLog> findByDealerIdOrderBySharedAtDesc(Long dealerId);

    // Count how many times a vehicle was shared
    long countByVehicleId(Long vehicleId);

    // Count successful shares for a vehicle
    long countByVehicleIdAndStatus(Long vehicleId, WhatsappMessageStatus status);

    // All successful shares for a vehicle
    List<WhatsappVehicleShareLog> findByVehicleIdAndStatus(
            Long vehicleId, WhatsappMessageStatus status);
}