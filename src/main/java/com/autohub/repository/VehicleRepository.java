package com.autohub.repository;

import com.autohub.entity.Vehicle;
import com.autohub.enums.VehicleStatus;
import com.autohub.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    long count();

    Long countByDealer_Id(Long dealerId);

    Long countByDealer_IdAndVehicleStatus(Long dealerId, VehicleStatus status);

    long countByDealerId(Long dealerId);

    List<Vehicle> findByDealerId(Long dealerId);

    List<Vehicle> findTop10ByVehicleStatusOrderByIdDesc(VehicleStatus vehicleStatus);

    List<Vehicle> findTop10ByVehicleStatusInOrderByCreatedAtDesc(
            List<VehicleStatus> statuses
    );


//    @Query("""
//       SELECT v
//       FROM Vehicle v
//       WHERE v.vehicleStatus IN ('ACTIVE', 'FEATURED')
//       AND v.vehicleType = :vehicleType
//       """)
//    Page<Vehicle> findAllActiveAndFeaturedVehicles(
//            @Param("vehicleType") VehicleType vehicleType,
//            Pageable pageable);

    @Query("""
       SELECT v
       FROM Vehicle v
       WHERE v.vehicleType = :vehicleType
       AND v.vehicleStatus = 'ACTIVE'
       """)
    List<Vehicle> findAllActiveAndFeaturedVehicles(
            @Param("vehicleType") VehicleType vehicleType);




}