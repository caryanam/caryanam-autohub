package com.autohub.repository;

import com.autohub.entity.SocialPostBatchItem;
import com.autohub.enums.SocialPostPublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialPostBatchItemRepository extends JpaRepository<SocialPostBatchItem, Long> {

    List<SocialPostBatchItem> findByBatch_IdOrderByIdAsc(Long batchId);

    List<SocialPostBatchItem> findByBatch_IdAndStatus(Long batchId, SocialPostPublishStatus status);

    /**
     * Used by the worker to pick up the next unit of work one at a time
     * (recommended concurrency of 1-2, per the queue design).
     */
    List<SocialPostBatchItem> findByStatusOrderByCreatedAtAsc(SocialPostPublishStatus status);
}
