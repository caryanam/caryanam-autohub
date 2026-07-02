package com.autohub.repository;

import com.autohub.entity.WhatsappMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsappMessageLogRepository extends JpaRepository<WhatsappMessageLog, Long> {

    List<WhatsappMessageLog> findByLeadId(Long leadId);

    List<WhatsappMessageLog> findByDealerIdOrderByCreatedAtDesc(Long dealerId);
}
