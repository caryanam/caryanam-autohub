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

    /**
     * Delay between each WhatsApp message send.
     * Meta's Cloud API rate limit is 80 messages/second per phone number ID
     * on most tiers. 500ms gap = max 2 msg/sec = well within limits even
     * at 1000+ dealers, and avoids any burst throttling.
     * Adjust down to 200ms if your Meta tier supports higher throughput
     * and you need faster broadcast completion.
     */
    private static final long DELAY_BETWEEN_SENDS_MS = 500;

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

        int totalDealers = activeDealers.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // ── Sequential send: one dealer at a time ──
        // This guarantees Meta's rate limits are never hit regardless
        // of dealer count, and gives us a clean per-dealer audit trail.
        for (int i = 0; i < totalDealers; i++) {

            Dealer dealer = activeDealers.get(i);
            int currentPosition = i + 1;

            log.info("Processing dealer [{}/{}] → id=[{}] name=[{}]",
                    currentPosition, totalDealers,
                    dealer.getId(), dealer.getOwnerName());

            // ── Validation ──
            if (!StringUtils.hasText(dealer.getWhatsapp())) {
                log.warn("Dealer [{}] '{}' has no WhatsApp number — skipping",
                        dealer.getId(), dealer.getOwnerName());

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

                // Still apply delay even on skip so we don't hammer
                // the log persistence layer either
                sleepBetweenSends(currentPosition, totalDealers);
                continue;
            }

            String normalizedNumber = normalizeToE164(dealer.getWhatsapp());

            try {
                // ── Send to this dealer ──
                WhatsAppOfferClient.OfferSendResult result =
                        whatsAppOfferClient.sendOfferTemplate(
                                normalizedNumber,
                                properties.offerTemplateName(),
                                properties.offerLanguageCode(),
                                event.metaImageHandle(),
                                dealer.getOwnerName(),   // {{1}}
                                event.offerDetails(),    // {{2}}
                                event.benefits(),        // {{3}}
                                event.contactInfo()      // {{4}}
                        );

                if (result.success()) {
                    successCount.incrementAndGet();
                    log.info("✓ [{}/{}] Sent to dealer [{}] '{}' → number=[{}] messageId=[{}]",
                            currentPosition, totalDealers,
                            dealer.getId(), dealer.getOwnerName(),
                            normalizedNumber, result.whatsappMessageId());
                } else {
                    failCount.incrementAndGet();
                    log.error("✗ [{}/{}] Failed for dealer [{}] '{}' → number=[{}] error=[{}]",
                            currentPosition, totalDealers,
                            dealer.getId(), dealer.getOwnerName(),
                            normalizedNumber, result.errorMessage());
                }

                // Persist log regardless of success/failure
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
                log.error("✗ [{}/{}] Unexpected error for dealer [{}] '{}': {}",
                        currentPosition, totalDealers,
                        dealer.getId(), dealer.getOwnerName(), ex.getMessage(), ex);

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

            // ── Delay before next send ──
            // Skip delay after the last dealer — no point waiting
            // when there's nobody left to send to
            sleepBetweenSends(currentPosition, totalDealers);
        }

        // ── Update offer broadcast stats ──
        updateOfferStats(event.offerId(), successCount.get(), failCount.get());

        log.info("=== OFFER BROADCAST COMPLETED === offerId=[{}] " +
                        "Total=[{}] ✓ Success=[{}] ✗ Failed=[{}]",
                event.offerId(), totalDealers,
                successCount.get(), failCount.get());
    }

    /**
     * Waits between sends. Skips the delay after the last dealer.
     * Swallows InterruptedException gracefully so one interrupted
     * sleep never aborts the entire broadcast loop.
     */
    private void sleepBetweenSends(int currentPosition, int totalDealers) {
        if (currentPosition >= totalDealers) {
            return; // last dealer — no delay needed
        }
        try {
            log.debug("Waiting {}ms before next send ({}/{} done)...",
                    DELAY_BETWEEN_SENDS_MS, currentPosition, totalDealers);
            Thread.sleep(DELAY_BETWEEN_SENDS_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Send delay interrupted at dealer [{}/{}] — continuing broadcast",
                    currentPosition, totalDealers);
        }
    }

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

            log.debug("Log persisted → offerId=[{}] dealerId=[{}] status=[{}]",
                    offerId, dealerId, status);

        } catch (Exception ex) {
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