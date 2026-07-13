package com.autohub.configuration;

import com.autohub.dto.DealerOfferBroadcastEvent;
import com.autohub.dto.WhatsAppProperties;
import com.autohub.entity.Dealer;
import com.autohub.entity.WhatsappOfferMessageLog;
import com.autohub.enums.WhatsappMessageStatus;
import com.autohub.repository.DealerOfferRepository;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.WhatsappOfferMessageLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class DealerOfferBroadcastEventListener {

    private final DealerRepository dealerRepository;
    private final WhatsAppOfferClient whatsAppOfferClient;
    private final WhatsappOfferMessageLogRepository offerMessageLogRepository;
    private final DealerOfferRepository dealerOfferRepository;
    private final WhatsAppProperties properties;

    public DealerOfferBroadcastEventListener(
            DealerRepository dealerRepository,
            WhatsAppOfferClient whatsAppOfferClient,
            WhatsappOfferMessageLogRepository offerMessageLogRepository,
            DealerOfferRepository dealerOfferRepository,
            WhatsAppProperties properties) {
        this.dealerRepository = dealerRepository;
        this.whatsAppOfferClient = whatsAppOfferClient;
        this.offerMessageLogRepository = offerMessageLogRepository;
        this.dealerOfferRepository = dealerOfferRepository;
        this.properties = properties;
    }

    @Async("whatsAppTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDealerOfferBroadcast(DealerOfferBroadcastEvent event) {

        log.info("=== OFFER BROADCAST STARTED === offerId=[{}] targeting [{}] dealers",
                event.offerId(), event.totalDealers());

        List<Dealer> activeDealers = dealerRepository.findAllActiveDealers();

        if (activeDealers.isEmpty()) {
            log.warn("No active dealers found for offer broadcast offerId=[{}]", event.offerId());
            return;
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (Dealer dealer : activeDealers) {

            // ── Validation: skip dealers with no WhatsApp number ──
            if (!StringUtils.hasText(dealer.getWhatsapp())) {
                log.warn("Dealer [{}] '{}' has no WhatsApp number — skipping",
                        dealer.getId(), dealer.getOwnerName());

                // Still persist a FAILED log so we have a complete audit trail
                persistOfferLog(
                        event.offerId(),
                        dealer.getId(),
                        dealer.getOwnerName(),
                        "N/A",
                        event.metaImageHandle(),
                        WhatsappMessageStatus.FAILED,
                        null,
                        null,
                        "Dealer has no WhatsApp number configured"
                );
                failCount.incrementAndGet();
                continue;
            }

            String normalizedNumber = normalizeToE164(dealer.getWhatsapp());

            try {
                WhatsAppOfferClient.OfferSendResult result = whatsAppOfferClient.sendOfferTemplate(
                        normalizedNumber,
                        properties.offerTemplateName(),
                        properties.offerLanguageCode(),
                        event.metaImageHandle(),
                        dealer.getOwnerName(),  // {{1}} — personalized per dealer
                        event.offerDetails(),   // {{2}}
                        event.benefits(),       // {{3}}
                        event.contactInfo()     // {{4}}
                );

                if (result.success()) {
                    successCount.incrementAndGet();
                    log.info("✓ Offer sent to dealer [{}] '{}' → messageId=[{}]",
                            dealer.getId(), dealer.getOwnerName(), result.whatsappMessageId());
                } else {
                    failCount.incrementAndGet();
                    log.error("✗ Offer failed for dealer [{}] '{}': {}",
                            dealer.getId(), dealer.getOwnerName(), result.errorMessage());
                }

                // Persist log for EVERY dealer regardless of success/failure
                persistOfferLog(
                        event.offerId(),
                        dealer.getId(),
                        dealer.getOwnerName(),
                        normalizedNumber,
                        event.metaImageHandle(),
                        result.success()
                                ? WhatsappMessageStatus.SUCCESS
                                : WhatsappMessageStatus.FAILED,
                        result.whatsappMessageId(),
                        result.responsePayload(),
                        result.errorMessage()
                );

            } catch (Exception ex) {
                failCount.incrementAndGet();
                log.error("Unexpected error sending offer to dealerId=[{}]: {}",
                        dealer.getId(), ex.getMessage(), ex);

                persistOfferLog(
                        event.offerId(),
                        dealer.getId(),
                        dealer.getOwnerName(),
                        normalizedNumber,
                        event.metaImageHandle(),
                        WhatsappMessageStatus.FAILED,
                        null,
                        null,
                        ex.getMessage()
                );
            }
        }

        // Update offer record with final broadcast statistics
        updateOfferStats(event.offerId(), successCount.get(), failCount.get());

        log.info("=== OFFER BROADCAST COMPLETED === offerId=[{}] ✓ Success=[{}] ✗ Failed=[{}]",
                event.offerId(), successCount.get(), failCount.get());
    }

    /**
     * Persists one log row per dealer per offer send attempt.
     * Uses REQUIRES_NEW so each log entry commits independently —
     * a failure saving one log row never blocks the next dealer's send.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistOfferLog(
            Long offerId,
            Long dealerId,
            String dealerName,
            String mobileNumber,
            String metaImageHandle,
            WhatsappMessageStatus status,
            String whatsappMessageId,
            String responsePayload,
            String errorMessage) {

        try {
            WhatsappOfferMessageLog logEntry = WhatsappOfferMessageLog.builder()
                    .offerId(offerId)
                    .dealerId(dealerId)
                    .dealerName(dealerName)
                    .mobileNumber(mobileNumber)
                    .templateName(properties.offerTemplateName())
                    .status(status)
                    .whatsappMessageId(whatsappMessageId)
                    .metaImageHandle(metaImageHandle)
                    .responsePayload(responsePayload)
                    .errorMessage(errorMessage)
                    .build();

            offerMessageLogRepository.save(logEntry);

            log.debug("Offer log persisted → offerId=[{}] dealerId=[{}] status=[{}]",
                    offerId, dealerId, status);

        } catch (Exception ex) {
            // Last-resort safety — log persistence failure must never
            // propagate and interrupt the broadcast loop
            log.error("Failed to persist offer log for offerId=[{}] dealerId=[{}]: {}",
                    offerId, dealerId, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOfferStats(Long offerId, int success, int failed) {
        dealerOfferRepository.findById(offerId).ifPresent(offer -> {
            offer.setTotalSentSuccess(success);
            offer.setTotalSentFailed(failed);
            dealerOfferRepository.save(offer);
            log.info("Offer stats updated → offerId=[{}] success=[{}] failed=[{}]",
                    offerId, success, failed);
        });
    }

    private String normalizeToE164(String rawNumber) {
        String digitsOnly = rawNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            return "91" + digitsOnly;
        }
        return digitsOnly;
    }
}