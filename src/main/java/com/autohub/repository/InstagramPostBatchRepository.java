package com.autohub.repository;

import com.autohub.entity.InstagramPostBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstagramPostBatchRepository extends JpaRepository<InstagramPostBatch, Long> {

    List<InstagramPostBatch> findByDealer_IdOrderByCreatedAtDesc(Long dealerId);
}
