package com.autohub.repository;

import com.autohub.entity.DealerWhatsappOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DealerWhatsappOtpRepository extends JpaRepository<DealerWhatsappOtpVerification, Long> {

    Optional<DealerWhatsappOtpVerification> findByWhatsapp(String whatsapp);

}
