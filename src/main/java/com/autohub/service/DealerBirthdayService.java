package com.autohub.service;

import com.autohub.configuration.WhatsAppBirthdayClient;
import com.autohub.entity.Dealer;
import com.autohub.repository.DealerRepository;
import com.autohub.entity.WhatsappBirthdayMessageLog;
import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.enums.WhatsappMessageStatus;
import com.autohub.dto.WhatsAppProperties;
import com.autohub.repository.WhatsappBirthdayMessageLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class DealerBirthdayService {

    private final DealerRepository dealerRepository;
    private final WhatsAppBirthdayClient birthdayClient;
    private final WhatsappBirthdayMessageLogRepository birthdayLogRepository;
    private final WhatsAppProperties properties;

    public DealerBirthdayService(DealerRepository dealerRepository,
                                 WhatsAppBirthdayClient birthdayClient,
                                 WhatsappBirthdayMessageLogRepository birthdayLogRepository,
                                 WhatsAppProperties properties) {
        this.dealerRepository = dealerRepository;
        this.birthdayClient = birthdayClient;
        this.birthdayLogRepository = birthdayLogRepository;
        this.properties = properties;
    }

    /**
     * Runs every day at 7:00 AM.
     * Finds dealers whose birthday is today and sends them a WhatsApp wish.
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void sendBirthdayWishes() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        log.info("Starting automated birthday wish broadcast for {}/{}", month, day);

        List<Dealer> birthdayDealers = dealerRepository.findApprovedDealersByBirthdayMonthAndDay(month, day);

        if (birthdayDealers.isEmpty()) {
            log.info("No approved dealers have a birthday today.");
            return;
        }

        log.info("Found {} dealers with a birthday today. Sending wishes...", birthdayDealers.size());

        for (Dealer dealer : birthdayDealers) {
            try {
                String toMobile = dealer.getWhatsapp();
                if (toMobile == null || toMobile.isBlank()) {
                    toMobile = dealer.getDealerMobile(); // fallback
                }
                
                if (toMobile != null && !toMobile.isBlank()) {
                    // Ensure the number has country code (like the rest of the app)
                    if (!toMobile.startsWith("91") && toMobile.length() == 10) {
                        toMobile = "91" + toMobile;
                    }

                    WhatsappBirthdayMessageLog logEntry = WhatsappBirthdayMessageLog.builder()
                            .dealerId(dealer.getId())
                            .dealerName(dealer.getOwnerName())
                            .mobileNumber(toMobile)
                            .templateName(properties.birthdayTemplateName())
                            .status(WhatsappMessageStatus.PENDING)
                            .build();

                    logEntry = birthdayLogRepository.save(logEntry);

                    WhatsAppBirthdayClient.BirthdaySendResult result = 
                            birthdayClient.sendBirthdayWish(toMobile, dealer.getOwnerName());

                    if (result.success()) {
                        logEntry.setStatus(WhatsappMessageStatus.SUCCESS);
                        logEntry.setWhatsappMessageId(result.whatsappMessageId());
                        logEntry.setResponsePayload(result.responsePayload());
                        log.info("Successfully sent birthday wish to dealer [{}] ({})", 
                                dealer.getOwnerName(), toMobile);
                    } else {
                        logEntry.setStatus(WhatsappMessageStatus.FAILED);
                        logEntry.setDeliveryStatus(WhatsappDeliveryStatus.FAILED);
                        logEntry.setErrorMessage(result.errorMessage());
                        logEntry.setResponsePayload(result.responsePayload());
                        log.error("Failed to send birthday wish to dealer [{}]. Error: {}", 
                                dealer.getOwnerName(), result.errorMessage());
                    }
                    
                    birthdayLogRepository.save(logEntry);
                }
            } catch (Exception e) {
                log.error("Error sending birthday wish to dealer ID [{}]: {}", dealer.getId(), e.getMessage());
            }
        }

        log.info("Completed automated birthday wish broadcast.");
    }
}
