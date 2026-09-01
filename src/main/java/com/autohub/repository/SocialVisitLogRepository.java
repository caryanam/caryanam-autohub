package com.autohub.repository;

import com.autohub.entity.SocialVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialVisitLogRepository extends JpaRepository<SocialVisitLog, Long> {
    List<SocialVisitLog> findAllByOrderByVisitedAtDesc();
    List<SocialVisitLog> findByVehicle_IdOrderByVisitedAtDesc(Long vehicleId);
    List<SocialVisitLog> findByDealer_IdOrderByVisitedAtDesc(Long dealerId);
}
