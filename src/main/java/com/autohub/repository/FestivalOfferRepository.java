package com.autohub.repository;

import com.autohub.entity.FestivalOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FestivalOfferRepository extends JpaRepository<FestivalOffer, Long> {
    Optional<FestivalOffer> findFirstByEnabledTrueOrderByCreatedAtDesc();
    List<FestivalOffer> findByExpiresAtBefore(LocalDateTime time);
}
