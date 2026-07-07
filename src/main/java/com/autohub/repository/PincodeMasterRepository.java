package com.autohub.repository;

import com.autohub.entity.PincodeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PincodeMasterRepository
        extends JpaRepository<PincodeMaster, Long> {

    @Query("""
            SELECT DISTINCT p.city
            FROM PincodeMaster p
            ORDER BY p.city
            """)
    List<String> getAllCities();

    @Query("""
            SELECT DISTINCT p.area
            FROM PincodeMaster p
            WHERE p.city = :city
            ORDER BY p.area
            """)
    List<String> getAreasByCity(String city);

 //   Optional<PincodeMaster> findByArea(String area);

    List<PincodeMaster> findAllByArea(String area);

    List<PincodeMaster> findAllByPincode(String pincode);

    @Query("""
       SELECT DISTINCT p.area
       FROM PincodeMaster p
       WHERE p.area IS NOT NULL
       AND p.area <> ''
       ORDER BY p.area
       """)
    List<String> getAllAreas();


}