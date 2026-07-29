package com.autohub.repository;

import com.autohub.entity.WhatsappOfferMessageLog;
import com.autohub.enums.WhatsappMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsappOfferMessageLogRepository
        extends JpaRepository<WhatsappOfferMessageLog, Long> {

    // All logs for a specific offer broadcast
    List<WhatsappOfferMessageLog> findByOfferIdOrderByCreatedAtAsc(Long offerId);

    // All offers sent to a specific dealer
    List<WhatsappOfferMessageLog> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    // All failed sends for a specific offer (useful for re-send logic later)
    List<WhatsappOfferMessageLog> findByOfferIdAndStatus(
            Long offerId, WhatsappMessageStatus status);

    // Count success/fail for an offer
    long countByOfferIdAndStatus(Long offerId, WhatsappMessageStatus status);

    // For webhook update
    List<WhatsappOfferMessageLog> findByWhatsappMessageId(String whatsappMessageId);

    // For failed messages list
    @Query("SELECT l FROM WhatsappOfferMessageLog l WHERE l.status = 'FAILED' OR l.deliveryStatus = 'FAILED' ORDER BY l.createdAt DESC")
    List<WhatsappOfferMessageLog> findAllFailed();
}