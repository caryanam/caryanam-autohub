package com.autohub.repository;

import com.autohub.entity.VehicleMedia;
import com.autohub.entity.VehicleView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleViewRepository extends JpaRepository<VehicleView, Long> {

    // Month Wise Views

    @Query("""
        SELECT MONTH(v.viewedAt),
               COUNT(v)
        FROM VehicleView v
        WHERE v.dealer.id = :dealerId
        AND YEAR(v.viewedAt) = YEAR(CURRENT_DATE)
        GROUP BY MONTH(v.viewedAt)
        ORDER BY MONTH(v.viewedAt)
    """)
    List<Object[]> getMonthlyViews(@Param("dealerId") Long dealerId);


    // Total Views

    @Query("""
            SELECT COUNT(vv)
            FROM VehicleView vv
            WHERE vv.vehicle.dealer.id = :dealerId
            """)
    long countViewsByDealerId(@Param("dealerId") Long dealerId);

    void deleteByVehicleId(Long vehicleId);



}