package com.autohub.service;

import com.autohub.dto.*;

import java.util.List;

public interface FacebookPublishAdminService {

    List<FacebookAdminDealerSummaryDTO> getDealerSummaries();

    List<FacebookAdminVehicleRequestDTO> getDealerRequests(Long dealerId);

    void rejectRequests(FacebookRejectRequestDTO request, Long adminId);

    /**
     * The full "Approve &amp; Publish Selected Vehicles" flow: locks the
     * requests, validates them, creates a batch + queue items, commits,
     * then hands off to the async worker. Returns immediately - does not
     * wait for Facebook publishing to complete.
     */
    FacebookBulkApprovePublishResponseDTO bulkApproveAndPublish(
            FacebookBulkApprovePublishRequestDTO request,
            Long adminId);

    FacebookBatchStatusDTO getBatchStatus(Long batchId);

    FacebookBulkApprovePublishResponseDTO retryFailed(FacebookRetryFailedRequestDTO request, Long adminId);
}
