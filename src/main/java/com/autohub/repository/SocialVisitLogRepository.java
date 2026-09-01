package com.autohub.repository;

import com.autohub.entity.SocialVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SocialVisitLogRepository extends JpaRepository<SocialVisitLog, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM SocialVisitLog i WHERE i.vehicle.id = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") Long vehicleId);
    List<SocialVisitLog> findAllByOrderByVisitedAtDesc();
    List<SocialVisitLog> findByVehicle_IdOrderByVisitedAtDesc(Long vehicleId);
    List<SocialVisitLog> findByDealer_IdOrderByVisitedAtDesc(Long dealerId);
}
