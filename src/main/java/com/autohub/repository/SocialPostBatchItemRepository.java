package com.autohub.repository;

import com.autohub.entity.SocialPostBatchItem;
import com.autohub.enums.SocialPostPublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SocialPostBatchItemRepository extends JpaRepository<SocialPostBatchItem, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM SocialPostBatchItem i WHERE i.vehicle.id = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    List<SocialPostBatchItem> findByBatch_IdOrderByIdAsc(Long batchId);

    List<SocialPostBatchItem> findByBatch_IdAndStatus(Long batchId, SocialPostPublishStatus status);

    /**
     * Used by the worker to pick up the next unit of work one at a time
     * (recommended concurrency of 1-2, per the queue design).
     */
    List<SocialPostBatchItem> findByStatusOrderByCreatedAtAsc(SocialPostPublishStatus status);
}
