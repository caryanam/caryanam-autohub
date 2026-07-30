package com.autohub.repository;

import com.autohub.entity.WhatsappBirthdayMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsappBirthdayMessageLogRepository extends JpaRepository<WhatsappBirthdayMessageLog, Long> {

    Optional<WhatsappBirthdayMessageLog> findByWhatsappMessageId(String whatsappMessageId);

    @Query("SELECT l FROM WhatsappBirthdayMessageLog l WHERE l.status = 'FAILED' OR l.deliveryStatus = 'FAILED'")
    List<WhatsappBirthdayMessageLog> findAllFailed();
}
