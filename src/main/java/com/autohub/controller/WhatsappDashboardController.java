package com.autohub.controller;

import com.autohub.dto.WhatsappDashboardStatsDTO;
import com.autohub.service.WhatsappDashboardService;
import com.autohub.service.WhatsappRetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Frontend-facing APIs for WhatsApp message tracking dashboard.
 *
 * ALL endpoints here are for your frontend admin panel.
 * (The webhook endpoints in WhatsAppWebhookController are for Meta only.)
 *
 * Base URL: /api/admin/whatsapp
 */
@RestController
@RequestMapping("/api/admin/whatsapp")
@Slf4j
public class WhatsappDashboardController {

    private final WhatsappDashboardService dashboardService;
    private final WhatsappRetryService retryService;

    public WhatsappDashboardController(
            WhatsappDashboardService dashboardService,
            WhatsappRetryService retryService) {
        this.dashboardService = dashboardService;
        this.retryService = retryService;
    }

    /**
     * GET /api/admin/whatsapp/dashboard
     *
     * Returns overall stats across ALL templates.
     * Use this for the main WhatsApp dashboard page.
     *
     * Frontend usage: Admin dashboard home → WhatsApp section
     *
     * Response includes:
     * - totalMessagesSent, totalDelivered, totalRead, totalFailed
     * - overallDeliveryRate, overallReadRate (percentages)
     * - Breakdown per template: lead, offer, vehicle
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.OverallStats> getDashboard() {
        log.info("Dashboard stats requested");
        return ResponseEntity.ok(dashboardService.getOverallStats());
    }

    /**
     * GET /api/admin/whatsapp/leads/stats
     *
     * Stats for lead_notification template only.
     * Frontend usage: Lead management page → WhatsApp stats section
     */
    @GetMapping("/leads/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.TemplateStats> getLeadStats() {
        return ResponseEntity.ok(dashboardService.getLeadNotificationStats());
    }

    /**
     * GET /api/admin/whatsapp/offers/stats
     *
     * Stats for caryanam_dealer_offers template only.
     * Frontend usage: Offer management page → WhatsApp delivery stats
     */
    @GetMapping("/offers/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.TemplateStats> getOfferStats() {
        return ResponseEntity.ok(dashboardService.getOfferBroadcastStats());
    }

    /**
     * GET /api/admin/whatsapp/vehicles/stats
     *
     * Stats for caryanam_dealer_vehicles_catalog template only.
     * Frontend usage: Vehicle management page → share stats
     */
    @GetMapping("/vehicles/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.TemplateStats> getVehicleStats() {
        return ResponseEntity.ok(dashboardService.getVehicleShareStats());
    }

    /**
     * GET /api/admin/whatsapp/birthdays/stats
     *
     * Stats for dealer_birthday_wish template only.
     * Frontend usage: Birthday wishes stats
     */
    @GetMapping("/birthdays/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.TemplateStats> getBirthdayStats() {
        return ResponseEntity.ok(dashboardService.getBirthdayStats());
    }

    /**
     * GET /api/admin/whatsapp/failed-messages
     *
     * All failed messages across ALL three templates, sorted newest first.
     * Frontend usage: "Failed Messages" table with Retry button per row.
     *
     * Response fields per message:
     * - logId, logType (LEAD/OFFER/VEHICLE)
     * - dealerName, mobileNumber, templateName
     * - apiStatus (SUCCESS/FAILED — whether API call worked)
     * - deliveryStatus (ACCEPTED/SENT/DELIVERED/READ/FAILED)
     * - errorMessage, retryCount, canRetry, createdAt, lastRetryAt
     */
    @GetMapping("/failed-messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WhatsappDashboardStatsDTO.FailedMessageDTO>> getFailedMessages() {
        return ResponseEntity.ok(dashboardService.getAllFailedMessages());
    }

    /**
     * GET /api/admin/whatsapp/logs/leads
     * Returns all lead logs (sorted newest first)
     */
    @GetMapping("/logs/leads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.autohub.entity.WhatsappMessageLog>> getLeadLogs() {
        return ResponseEntity.ok(dashboardService.getLeadLogs());
    }

    /**
     * GET /api/admin/whatsapp/logs/offers
     * Returns all offer logs (sorted newest first)
     */
    @GetMapping("/logs/offers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.autohub.entity.WhatsappOfferMessageLog>> getOfferLogs() {
        return ResponseEntity.ok(dashboardService.getOfferLogs());
    }

    /**
     * GET /api/admin/whatsapp/logs/vehicles
     * Returns all vehicle logs (sorted newest first)
     */
    @GetMapping("/logs/vehicles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.autohub.entity.WhatsappVehicleShareLog>> getVehicleLogs() {
        return ResponseEntity.ok(dashboardService.getVehicleLogs());
    }

    /**
     * GET /api/admin/whatsapp/logs/birthdays
     * Returns all birthday logs (sorted newest first)
     */
    @GetMapping("/logs/birthdays")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.autohub.entity.WhatsappBirthdayMessageLog>> getBirthdayLogs() {
        return ResponseEntity.ok(dashboardService.getBirthdayLogs());
    }

    /**
     * GET /api/admin/whatsapp/offers/{offerId}/delivery-summary
     *
     * Full per-dealer delivery breakdown for a specific offer broadcast.
     * Frontend usage: Offer detail page → "Delivery Status" tab showing
     * each dealer's tick status (accepted/sent/delivered/read/failed).
     *
     * Example: Admin clicks on "July Festival Offer" → sees table with
     * each dealer and their delivery status with timestamps.
     */
    @GetMapping("/offers/{offerId}/delivery-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.OfferDeliverySummaryDTO> getOfferDeliverySummary(
            @PathVariable Long offerId) {
        return ResponseEntity.ok(dashboardService.getOfferDeliverySummary(offerId));
    }

    /**
     * POST /api/admin/whatsapp/retry/{logType}/{logId}
     *
     * Retries a single failed message. Max 3 retries per message.
     * After 3 retries, canRetry=false and this endpoint returns 400.
     *
     * Frontend usage: "Retry" button in the failed messages table.
     * Disable the button when canRetry=false.
     *
     * logType: LEAD, OFFER, or VEHICLE
     * logId:   the id from the respective log table
     *
     * Example: POST /api/admin/whatsapp/retry/OFFER/5
     *   → Retries the offer message log row with id=5
     */
    @PostMapping("/retry/{logType}/{logId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WhatsappDashboardStatsDTO.RetryResultDTO> retryFailedMessage(
            @PathVariable String logType,
            @PathVariable Long logId) {

        log.info("Manual retry requested → logType=[{}] logId=[{}]", logType, logId);

        try {
            WhatsappDashboardStatsDTO.RetryResultDTO result =
                    retryService.retryFailedMessage(logType, logId);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                // Retry attempted but Meta returned failure
                return ResponseEntity.status(200).body(result);
            }
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    WhatsappDashboardStatsDTO.RetryResultDTO.builder()
                            .logId(logId)
                            .logType(logType)
                            .success(false)
                            .retryCount(0)
                            .message(ex.getMessage())
                            .build());
        } catch (RuntimeException ex) {
            // Includes "max retry limit reached" case
            return ResponseEntity.badRequest().body(
                    WhatsappDashboardStatsDTO.RetryResultDTO.builder()
                            .logId(logId)
                            .logType(logType)
                            .success(false)
                            .retryCount(0)
                            .message(ex.getMessage())
                            .build());
        }
    }
}