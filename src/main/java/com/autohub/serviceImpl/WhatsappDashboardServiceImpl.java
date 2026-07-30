package com.autohub.serviceImpl;

import com.autohub.dto.WhatsappDashboardStatsDTO;
import com.autohub.dto.WhatsAppProperties;
import com.autohub.entity.WhatsappMessageLog;
import com.autohub.entity.WhatsappOfferMessageLog;
import com.autohub.entity.WhatsappVehicleShareLog;
import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.enums.WhatsappMessageStatus;
import com.autohub.repository.DealerOfferRepository;
import com.autohub.repository.WhatsappMessageLogRepository;
import com.autohub.repository.WhatsappOfferMessageLogRepository;
import com.autohub.repository.WhatsappVehicleShareLogRepository;
import com.autohub.service.WhatsappDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WhatsappDashboardServiceImpl implements WhatsappDashboardService {

    private final WhatsappMessageLogRepository messageLogRepository;
    private final WhatsappOfferMessageLogRepository offerMessageLogRepository;
    private final WhatsappVehicleShareLogRepository vehicleShareLogRepository;
    private final com.autohub.repository.WhatsappBirthdayMessageLogRepository birthdayMessageLogRepository;
    private final DealerOfferRepository dealerOfferRepository;
    private final WhatsAppProperties properties;

    public WhatsappDashboardServiceImpl(
            WhatsappMessageLogRepository messageLogRepository,
            WhatsappOfferMessageLogRepository offerMessageLogRepository,
            WhatsappVehicleShareLogRepository vehicleShareLogRepository,
            com.autohub.repository.WhatsappBirthdayMessageLogRepository birthdayMessageLogRepository,
            DealerOfferRepository dealerOfferRepository,
            WhatsAppProperties properties) {
        this.messageLogRepository = messageLogRepository;
        this.offerMessageLogRepository = offerMessageLogRepository;
        this.vehicleShareLogRepository = vehicleShareLogRepository;
        this.birthdayMessageLogRepository = birthdayMessageLogRepository;
        this.dealerOfferRepository = dealerOfferRepository;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappDashboardStatsDTO.OverallStats getOverallStats() {

        WhatsappDashboardStatsDTO.TemplateStats leads   = getLeadNotificationStats();
        WhatsappDashboardStatsDTO.TemplateStats offers  = getOfferBroadcastStats();
        WhatsappDashboardStatsDTO.TemplateStats vehicles = getVehicleShareStats();
        WhatsappDashboardStatsDTO.TemplateStats birthdays = getBirthdayStats();

        long totalSent      = leads.getTotalSent() + offers.getTotalSent() + vehicles.getTotalSent() + birthdays.getTotalSent();
        long totalDelivered = leads.getDelivered() + offers.getDelivered() + vehicles.getDelivered() + birthdays.getDelivered();
        long totalRead      = leads.getRead() + offers.getRead() + vehicles.getRead() + birthdays.getRead();
        long totalFailed    = leads.getFailed() + offers.getFailed() + vehicles.getFailed() + birthdays.getFailed();
        long totalAccepted  = leads.getAccepted() + offers.getAccepted() + vehicles.getAccepted() + birthdays.getAccepted();
        long totalSentState = leads.getSent() + offers.getSent() + vehicles.getSent() + birthdays.getSent();

        double deliveryRate = totalSent > 0
                ? Math.round((double) totalDelivered / totalSent * 1000.0) / 10.0
                : 0.0;
        double readRate = totalDelivered > 0
                ? Math.round((double) totalRead / totalDelivered * 1000.0) / 10.0
                : 0.0;

        return WhatsappDashboardStatsDTO.OverallStats.builder()
                .totalMessagesSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalRead(totalRead)
                .totalFailed(totalFailed)
                .totalAccepted(totalAccepted)
                .totalSent(totalSentState)
                .overallDeliveryRate(deliveryRate)
                .overallReadRate(readRate)
                .leadNotifications(leads)
                .offerBroadcasts(offers)
                .vehicleShares(vehicles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappDashboardStatsDTO.TemplateStats getLeadNotificationStats() {

        List<WhatsappMessageLog> all = messageLogRepository.findAll();

        long total     = all.size();
        long accepted  = countByDelivery(all.stream().map(l -> l.getDeliveryStatus()).toList(), WhatsappDeliveryStatus.ACCEPTED);
        long sent      = countByDelivery(all.stream().map(l -> l.getDeliveryStatus()).toList(), WhatsappDeliveryStatus.SENT);
        long delivered = countByDelivery(all.stream().map(l -> l.getDeliveryStatus()).toList(), WhatsappDeliveryStatus.DELIVERED);
        long read      = countByDelivery(all.stream().map(l -> l.getDeliveryStatus()).toList(), WhatsappDeliveryStatus.READ);
        long failed    = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.FAILED ||
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.FAILED).count();
        // In queue = accepted but API status is SUCCESS (sent to Meta but no delivery confirmation)
        long inQueue   = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.SUCCESS &&
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();

        double deliveryRate = total > 0
                ? Math.round((double) delivered / total * 1000.0) / 10.0 : 0.0;
        double readRate = delivered > 0
                ? Math.round((double) read / delivered * 1000.0) / 10.0 : 0.0;

        return WhatsappDashboardStatsDTO.TemplateStats.builder()
                .templateType("LEAD")
                .templateName(properties.templateName())
                .totalSent(total)
                .accepted(accepted)
                .sent(sent)
                .delivered(delivered)
                .read(read)
                .failed(failed)
                .inQueue(inQueue)
                .deliveryRate(deliveryRate)
                .readRate(readRate)
                .lastSentAt(all.isEmpty() ? null :
                        all.get(all.size() - 1).getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappDashboardStatsDTO.TemplateStats getOfferBroadcastStats() {

        List<WhatsappOfferMessageLog> all = offerMessageLogRepository.findAll();

        long total     = all.size();
        long accepted  = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();
        long sent      = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.SENT).count();
        long delivered = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.DELIVERED).count();
        long read      = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.READ).count();
        long failed    = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.FAILED ||
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.FAILED).count();
        long inQueue   = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.SUCCESS &&
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();

        double deliveryRate = total > 0
                ? Math.round((double) delivered / total * 1000.0) / 10.0 : 0.0;
        double readRate = delivered > 0
                ? Math.round((double) read / delivered * 1000.0) / 10.0 : 0.0;

        return WhatsappDashboardStatsDTO.TemplateStats.builder()
                .templateType("OFFER")
                .templateName(properties.offerTemplateName())
                .totalSent(total)
                .accepted(accepted)
                .sent(sent)
                .delivered(delivered)
                .read(read)
                .failed(failed)
                .inQueue(inQueue)
                .deliveryRate(deliveryRate)
                .readRate(readRate)
                .lastSentAt(all.isEmpty() ? null :
                        all.get(all.size() - 1).getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappDashboardStatsDTO.TemplateStats getVehicleShareStats() {

        List<WhatsappVehicleShareLog> all = vehicleShareLogRepository.findAll();

        long total     = all.size();
        long accepted  = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();
        long sent      = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.SENT).count();
        long delivered = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.DELIVERED).count();
        long read      = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.READ).count();
        long failed    = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.FAILED ||
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.FAILED).count();
        long inQueue   = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.SUCCESS &&
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();

        double deliveryRate = total > 0
                ? Math.round((double) delivered / total * 1000.0) / 10.0 : 0.0;
        double readRate = delivered > 0
                ? Math.round((double) read / delivered * 1000.0) / 10.0 : 0.0;

        return WhatsappDashboardStatsDTO.TemplateStats.builder()
                .templateType("VEHICLE")
                .templateName(properties.vehicleTemplateName())
                .totalSent(total)
                .accepted(accepted)
                .sent(sent)
                .delivered(delivered)
                .read(read)
                .failed(failed)
                .inQueue(inQueue)
                .deliveryRate(deliveryRate)
                .readRate(readRate)
                .lastSentAt(all.isEmpty() ? null :
                        all.get(all.size() - 1).getSharedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappDashboardStatsDTO.TemplateStats getBirthdayStats() {
        List<com.autohub.entity.WhatsappBirthdayMessageLog> all = birthdayMessageLogRepository.findAll();

        long total     = all.size();
        long accepted  = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();
        long sent      = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.SENT).count();
        long delivered = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.DELIVERED).count();
        long read      = all.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.READ).count();
        long failed    = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.FAILED ||
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.FAILED).count();
        long inQueue   = all.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.SUCCESS &&
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();

        double deliveryRate = total > 0
                ? Math.round((double) delivered / total * 1000.0) / 10.0 : 0.0;
        double readRate = delivered > 0
                ? Math.round((double) read / delivered * 1000.0) / 10.0 : 0.0;

        return WhatsappDashboardStatsDTO.TemplateStats.builder()
                .templateType("BIRTHDAY")
                .templateName(properties.birthdayTemplateName())
                .totalSent(total)
                .accepted(accepted)
                .sent(sent)
                .delivered(delivered)
                .read(read)
                .failed(failed)
                .inQueue(inQueue)
                .deliveryRate(deliveryRate)
                .readRate(readRate)
                .lastSentAt(all.isEmpty() ? null :
                        all.get(all.size() - 1).getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhatsappDashboardStatsDTO.FailedMessageDTO> getAllFailedMessages() {

        List<WhatsappDashboardStatsDTO.FailedMessageDTO> result = new ArrayList<>();

        // 1. Failed lead notifications
        messageLogRepository.findAllFailed().forEach(log ->
                result.add(WhatsappDashboardStatsDTO.FailedMessageDTO.builder()
                        .logId(log.getId())
                        .logType("LEAD")
                        .referenceId(log.getLeadId())
                        .dealerId(log.getDealerId())
                        .dealerName("Dealer #" + log.getDealerId())
                        .mobileNumber(log.getMobileNumber())
                        .templateName(log.getTemplateName())
                        .apiStatus(log.getStatus())
                        .deliveryStatus(log.getDeliveryStatus())
                        .errorMessage(log.getResponsePayload())
                        .responsePayload(log.getResponsePayload())
                        .retryCount(log.getRetryCount())
                        .createdAt(log.getCreatedAt())
                        .lastRetryAt(log.getLastRetryAt())
                        .canRetry(log.getRetryCount() < 3)
                        .build())
        );

        // 2. Failed offer broadcasts
        offerMessageLogRepository.findAllFailed().forEach(log ->
                result.add(WhatsappDashboardStatsDTO.FailedMessageDTO.builder()
                        .logId(log.getId())
                        .logType("OFFER")
                        .referenceId(log.getOfferId())
                        .dealerId(log.getDealerId())
                        .dealerName(log.getDealerName())
                        .mobileNumber(log.getMobileNumber())
                        .templateName(log.getTemplateName())
                        .apiStatus(log.getStatus())
                        .deliveryStatus(log.getDeliveryStatus())
                        .errorMessage(log.getErrorMessage())
                        .responsePayload(log.getResponsePayload())
                        .retryCount(log.getRetryCount())
                        .createdAt(log.getCreatedAt())
                        .lastRetryAt(log.getLastRetryAt())
                        .canRetry(log.getRetryCount() < 3)
                        .build())
        );

        // 3. Failed vehicle shares
        vehicleShareLogRepository.findAllFailed().forEach(log ->
                result.add(WhatsappDashboardStatsDTO.FailedMessageDTO.builder()
                        .logId(log.getId())
                        .logType("VEHICLE")
                        .referenceId(log.getVehicleId())
                        .dealerId(log.getDealerId())
                        .dealerName(log.getDealerName())
                        .mobileNumber(log.getSentToNumber())
                        .templateName(log.getTemplateName())
                        .apiStatus(log.getStatus())
                        .deliveryStatus(log.getDeliveryStatus())
                        .errorMessage(log.getErrorMessage())
                        .responsePayload(log.getResponsePayload())
                        .retryCount(log.getRetryCount())
                        .createdAt(log.getSharedAt())
                        .lastRetryAt(log.getLastRetryAt())
                        .canRetry(log.getRetryCount() < 3)
                        .build())
        );

        // 4. Failed birthday wishes
        birthdayMessageLogRepository.findAllFailed().forEach(log ->
                result.add(WhatsappDashboardStatsDTO.FailedMessageDTO.builder()
                        .logId(log.getId())
                        .logType("BIRTHDAY")
                        .referenceId(log.getDealerId())
                        .dealerId(log.getDealerId())
                        .dealerName(log.getDealerName())
                        .mobileNumber(log.getMobileNumber())
                        .templateName(log.getTemplateName())
                        .apiStatus(log.getStatus().name())
                        .deliveryStatus(log.getDeliveryStatus().name())
                        .errorMessage(log.getErrorMessage())
                        .responsePayload(log.getResponsePayload())
                        .retryCount(log.getRetryCount())
                        .createdAt(log.getCreatedAt())
                        .lastRetryAt(log.getLastRetryAt())
                        .canRetry(log.getRetryCount() < 3)
                        .build())
        );

        // Sort all by createdAt descending
        result.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappDashboardStatsDTO.OfferDeliverySummaryDTO getOfferDeliverySummary(Long offerId) {

        List<WhatsappOfferMessageLog> logs =
                offerMessageLogRepository.findByOfferIdOrderByCreatedAtAsc(offerId);

        long accepted  = logs.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.ACCEPTED).count();
        long sent      = logs.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.SENT).count();
        long delivered = logs.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.DELIVERED).count();
        long read      = logs.stream().filter(l -> l.getDeliveryStatus() == WhatsappDeliveryStatus.READ).count();
        long failed    = logs.stream().filter(l ->
                l.getStatus() == WhatsappMessageStatus.FAILED ||
                        l.getDeliveryStatus() == WhatsappDeliveryStatus.FAILED).count();

        double deliveryRate = !logs.isEmpty()
                ? Math.round((double) delivered / logs.size() * 1000.0) / 10.0 : 0.0;

        List<WhatsappDashboardStatsDTO.DealerDeliveryStatus> breakdown = logs.stream()
                .map(log -> WhatsappDashboardStatsDTO.DealerDeliveryStatus.builder()
                        .dealerId(log.getDealerId())
                        .dealerName(log.getDealerName())
                        .mobileNumber(log.getMobileNumber())
                        .deliveryStatus(log.getDeliveryStatus())
                        .whatsappMessageId(log.getWhatsappMessageId())
                        .sentAt(log.getCreatedAt())
                        .build())
                .toList();

        // Get offer title from dealer_offers table
        String offerTitle = dealerOfferRepository.findById(offerId)
                .map(o -> o.getOfferTitle())
                .orElse("Offer #" + offerId);

        return WhatsappDashboardStatsDTO.OfferDeliverySummaryDTO.builder()
                .offerId(offerId)
                .offerTitle(offerTitle)
                .totalDealers(logs.size())
                .accepted(accepted)
                .sent(sent)
                .delivered(delivered)
                .read(read)
                .failed(failed)
                .deliveryRate(deliveryRate)
                .dealerBreakdown(breakdown)
                .build();
    }

    private long countByDelivery(List<WhatsappDeliveryStatus> statuses,
                                 WhatsappDeliveryStatus target) {
        return statuses.stream().filter(s -> s == target).count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhatsappMessageLog> getLeadLogs() {
        return messageLogRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhatsappOfferMessageLog> getOfferLogs() {
        return offerMessageLogRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhatsappVehicleShareLog> getVehicleLogs() {
        return vehicleShareLogRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getSharedAt() == null) return 1;
                    if (b.getSharedAt() == null) return -1;
                    return b.getSharedAt().compareTo(a.getSharedAt());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.autohub.entity.WhatsappBirthdayMessageLog> getBirthdayLogs() {
        return birthdayMessageLogRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
    }
}