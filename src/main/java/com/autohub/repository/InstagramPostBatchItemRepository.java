package com.autohub.repository;

import com.autohub.entity.InstagramPostBatchItem;
import com.autohub.enums.SocialPostPublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstagramPostBatchItemRepository extends JpaRepository<InstagramPostBatchItem, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM InstagramPostBatchItem i WHERE i.vehicle.id = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    List<InstagramPostBatchItem> findByBatch_IdOrderByIdAsc(Long batchId);

    List<InstagramPostBatchItem> findByBatch_IdAndStatus(Long batchId, SocialPostPublishStatus status);

    List<InstagramPostBatchItem> findByStatusOrderByCreatedAtAsc(SocialPostPublishStatus status);
}
