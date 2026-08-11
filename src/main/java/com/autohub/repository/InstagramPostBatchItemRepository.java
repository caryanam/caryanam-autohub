package com.autohub.repository;

import com.autohub.entity.InstagramPostBatchItem;
import com.autohub.enums.SocialPostPublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstagramPostBatchItemRepository extends JpaRepository<InstagramPostBatchItem, Long> {

    List<InstagramPostBatchItem> findByBatch_IdOrderByIdAsc(Long batchId);

    List<InstagramPostBatchItem> findByBatch_IdAndStatus(Long batchId, SocialPostPublishStatus status);

    List<InstagramPostBatchItem> findByStatusOrderByCreatedAtAsc(SocialPostPublishStatus status);
}
