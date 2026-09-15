package com.autohub.repository;

import com.autohub.entity.OfferVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferVideoRepository extends JpaRepository<OfferVideo, Long> {

    /**
     * Returns the currently active (enabled) offer video.
     * Only one should be active at a time — this picks the most recent.
     */
    Optional<OfferVideo> findFirstByEnabledTrueOrderByCreatedAtDesc();

    /**
     * Returns all offer videos ordered by most recent first.
     */
    List<OfferVideo> findAllByOrderByCreatedAtDesc();

    /**
     * Disable all currently enabled videos in a single query.
     * Called before enabling a new one to enforce the "only one active" rule.
     */
    @Modifying
    @Query("UPDATE OfferVideo o SET o.enabled = false WHERE o.enabled = true")
    void disableAllEnabled();
}
