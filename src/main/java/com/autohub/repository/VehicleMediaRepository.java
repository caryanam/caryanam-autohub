package com.autohub.repository;

import com.autohub.entity.VehicleMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleMediaRepository extends JpaRepository<VehicleMedia, Long> {


    List<VehicleMedia> findByVehicleId(Long vehicleId);
}
