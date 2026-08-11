package com.autohub.controller;

import com.autohub.configuration.CustomUserDetails;
import com.autohub.dto.*;
import com.autohub.service.InstagramPublishAdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-facing endpoints for the "Admin Dashboard -> Social Media ->
 * Instagram Requests" screen: dealer summary, per-dealer request review,
 * approve/reject, the main "Approve & Publish Selected Vehicles" action,
 * live batch status, and retry-failed.
 */
@RestController
@RequestMapping("/api/admin/instagram-post-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InstagramPublishAdminController {

    private final InstagramPublishAdminService instagramPublishAdminService;

    @Operation(summary = "Per-dealer summary counts: pending, processing, published, failed")
    @GetMapping("/dealer-summary")
    public ResponseEntity<ResponseDto<List<InstagramAdminDealerSummaryDTO>>> getDealerSummaries() {
        return ResponseEntity.ok(new ResponseDto<>(200, "Dealer summary fetched successfully",
                instagramPublishAdminService.getDealerSummaries()));
    }

    @Operation(summary = "Vehicle-level Instagram post requests for one dealer")
    @GetMapping("/dealer/{dealerId}")
    public ResponseEntity<ResponseDto<List<InstagramAdminVehicleRequestDTO>>> getDealerRequests(
            @PathVariable Long dealerId) {
        return ResponseEntity.ok(new ResponseDto<>(200, "Dealer requests fetched successfully",
                instagramPublishAdminService.getDealerRequests(dealerId)));
    }

    @Operation(summary = "Reject one or more pending requests")
    @PostMapping("/reject")
    public ResponseEntity<ResponseDto<Void>> rejectRequests(
            @Valid @RequestBody InstagramRejectRequestDTO request,
            Authentication authentication) {

        instagramPublishAdminService.rejectRequests(request, currentAdminId(authentication));
        return ResponseEntity.ok(new ResponseDto<>(200, "Requests rejected successfully", null));
    }

    @Operation(summary = "Approve & Publish Selected Vehicles - returns immediately, "
            + "does not wait for Instagram publishing to complete")
    @PostMapping("/bulk-approve-publish")
    public ResponseEntity<ResponseDto<InstagramBulkApprovePublishResponseDTO>> bulkApproveAndPublish(
            @Valid @RequestBody InstagramBulkApprovePublishRequestDTO request,
            Authentication authentication) {

        InstagramBulkApprovePublishResponseDTO response = instagramPublishAdminService
                .bulkApproveAndPublish(request, currentAdminId(authentication));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ResponseDto<>(202, "Batch approved and queued for publishing", response));
    }

    @Operation(summary = "Live batch progress - published / processing / failed, vehicle-wise")
    @GetMapping("/batch/{batchId}/status")
    public ResponseEntity<ResponseDto<InstagramBatchStatusDTO>> getBatchStatus(@PathVariable Long batchId) {
        return ResponseEntity.ok(new ResponseDto<>(200, "Batch status fetched successfully",
                instagramPublishAdminService.getBatchStatus(batchId)));
    }

    @Operation(summary = "Retry all retryable failed items in a batch")
    @PostMapping("/retry-failed")
    public ResponseEntity<ResponseDto<InstagramBulkApprovePublishResponseDTO>> retryFailed(
            @Valid @RequestBody InstagramRetryFailedRequestDTO request,
            Authentication authentication) {

        InstagramBulkApprovePublishResponseDTO response =
                instagramPublishAdminService.retryFailed(request, currentAdminId(authentication));

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ResponseDto<>(202, "Retry queued for failed items", response));
    }

    private Long currentAdminId(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return user.getId();
    }
}
