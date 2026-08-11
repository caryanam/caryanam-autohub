package com.autohub.service;

/**
 * Background worker that actually talks to the Instagram Graph API.
 * Triggered by InstagramPublishAdminService after a batch is committed -
 * never called synchronously from a controller.
 */
public interface InstagramPublishWorkerService {

    void processBatchAsync(Long batchId);

    void retryBatchAsync(Long batchId);
}
