package com.autohub.service;

import com.autohub.dto.*;

import java.util.List;

public interface InstagramPublishAdminService {

    List<InstagramAdminDealerSummaryDTO> getDealerSummaries();

    List<InstagramAdminVehicleRequestDTO> getDealerRequests(Long dealerId);

    void rejectRequests(InstagramRejectRequestDTO request, Long adminId);

    /**
     * The full "Approve &amp; Publish Selected Vehicles" flow: locks the
     * requests, validates them, creates a batch + queue items, commits,
     * then hands off to the async worker. Returns immediately - does not
     * wait for Instagram publishing to complete.
     */
    InstagramBulkApprovePublishResponseDTO bulkApproveAndPublish(
            InstagramBulkApprovePublishRequestDTO request,
            Long adminId);

    InstagramBatchStatusDTO getBatchStatus(Long batchId);

    InstagramBulkApprovePublishResponseDTO retryFailed(InstagramRetryFailedRequestDTO request, Long adminId);
}
