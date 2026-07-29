package com.autohub.repository;

import com.autohub.entity.WhatsappMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsappMessageLogRepository extends JpaRepository<WhatsappMessageLog, Long> {

    List<WhatsappMessageLog> findByLeadId(Long leadId);

    List<WhatsappMessageLog> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    List<WhatsappMessageLog> findByWhatsappMessageId(String whatsappMessageId);


    // For failed messages list
    @Query("SELECT l FROM WhatsappMessageLog l WHERE l.status = 'FAILED' OR l.deliveryStatus = 'FAILED' ORDER BY l.createdAt DESC")
    List<WhatsappMessageLog> findAllFailed();
}
