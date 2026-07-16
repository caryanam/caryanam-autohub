package com.autohub.service;

/**
 * Background worker that actually talks to the Facebook Graph API.
 * Triggered by FacebookPublishAdminService after a batch is committed -
 * never called synchronously from a controller.
 * Full implementation (Graph API calls, retry/backoff, status updates)
 * is delivered in the Worker phase.
 */
public interface FacebookPublishWorkerService {

    void processBatchAsync(Long batchId);

    void retryBatchAsync(Long batchId);
}
