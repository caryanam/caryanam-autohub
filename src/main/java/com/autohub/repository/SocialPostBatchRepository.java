package com.autohub.repository;

import com.autohub.entity.SocialPostBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialPostBatchRepository extends JpaRepository<SocialPostBatch, Long> {

    List<SocialPostBatch> findByDealer_IdOrderByCreatedAtDesc(Long dealerId);
}
