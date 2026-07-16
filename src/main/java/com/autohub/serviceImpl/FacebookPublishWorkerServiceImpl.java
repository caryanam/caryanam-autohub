package com.autohub.serviceImpl;

import com.autohub.configuration.FacebookGraphClient;
import com.autohub.dto.FacebookProperties;
import com.autohub.entity.*;
import com.autohub.enums.SocialPostBatchStatus;
import com.autohub.enums.SocialPostPublishStatus;
import com.autohub.exception.FacebookApiException;
import com.autohub.repository.SocialPostBatchItemRepository;
import com.autohub.repository.SocialPostBatchRepository;
import com.autohub.repository.VehicleRepository;
import com.autohub.repository.VehicleSocialPostRequestRepository;
import com.autohub.service.CaptionGeneratorService;
import com.autohub.service.FacebookPublishWorkerService;
import com.autohub.util.SocialPostVehicleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Walks the queue one item at a time (see FACEBOOK_BATCH_LIMIT / the
 * facebookTaskExecutor pool size for concurrency), publishing each vehicle
 * as its own Facebook Page post. Each item's outcome is persisted
 * independently as it happens - a failure on item 6 of 10 does not affect
 * items 1-5 already marked PUBLISHED (see "FAILED POSTS - Do NOT rollback"
 * in the spec).
 * Never called directly from a controller - only from
 * FacebookPublishAdminService, after a batch transaction has committed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookPublishWorkerServiceImpl implements FacebookPublishWorkerService {

    private final SocialPostBatchItemRepository batchItemRepository;
    private final SocialPostBatchRepository batchRepository;
    private final VehicleSocialPostRequestRepository requestRepository;
    private final VehicleRepository vehicleRepository;
    private final FacebookGraphClient facebookGraphClient;
    private final CaptionGeneratorService captionGeneratorService;
    private final FacebookProperties properties;

    @Value("${spring.server.url}")
    private String serverUrl;

    @Override
    @Async("facebookTaskExecutor")
    @Transactional
    public void processBatchAsync(Long batchId) {
        processQueuedItems(batchId);
    }

    @Override
    @Async("facebookTaskExecutor")
    @Transactional
    public void retryBatchAsync(Long batchId) {
        processQueuedItems(batchId);
    }


    private void processQueuedItems(Long batchId) {

        log.info("Worker starting batch [{}]", batchId);

        markBatchProcessing(batchId);

        List<SocialPostBatchItem> queuedItems = batchItemRepository.findByBatch_IdOrderByIdAsc(batchId)
                .stream()
                .filter(item -> item.getStatus() == SocialPostPublishStatus.QUEUED)
                .toList();

        // Sequential on purpose - "Facebook publishes one vehicle at a time".
        for (SocialPostBatchItem item : queuedItems) {
            processOneItem(item);
        }

        finalizeBatch(batchId);

        log.info("Worker finished batch [{}]", batchId);
    }

    private void processOneItem(SocialPostBatchItem item) {

        VehicleSocialPostRequest request = item.getRequest();

        try {
            // Reload the vehicle fresh in case it was deleted/edited after being queued.
            Vehicle vehicle = vehicleRepository.findById(item.getVehicle().getId()).orElse(null);
            if (vehicle == null) {
                failPermanently(item, request, "Vehicle was deleted after being queued");
                return;
            }

            VehicleMedia primaryImage = SocialPostVehicleUtil.findPrimaryImage(vehicle);
            if (primaryImage == null) {
                failPermanently(item, request, "Vehicle no longer has a usable image");
                return;
            }

            markProcessing(item, request);

            String imageUrl = SocialPostVehicleUtil.buildImageUrl(serverUrl, primaryImage);
            String caption = captionGeneratorService.generateCaption(vehicle);

            FacebookGraphClient.FacebookPublishResult result = facebookGraphClient.publishPhoto(imageUrl, caption);

            if (result.success()) {
                markPublished(item, request, result, caption);
            } else {
                // Retry attempts already exhausted inside FacebookGraphClient's
                // own retry template - this is a final transient failure for
                // this attempt cycle, eligible for the admin's "Retry Failed".
                markFailedTransient(item, request, result.errorMessage());
            }

        } catch (FacebookApiException ex) {
            if (ex.isPermanent()) {
                failPermanently(item, request, ex.getMessage());
            } else {
                markFailedTransient(item, request, ex.getMessage());
            }
        } catch (Exception ex) {
            log.error("Unexpected error publishing request [{}]", request.getId(), ex);
            markFailedTransient(item, request, "Unexpected error: " + ex.getMessage());
        }
    }

    // ---------------- status transitions ----------------

    private void markBatchProcessing(Long batchId) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            batch.setStatus(SocialPostBatchStatus.PROCESSING);
            batchRepository.save(batch);
        });
    }

    private void markProcessing(SocialPostBatchItem item, VehicleSocialPostRequest request) {
        item.setStatus(SocialPostPublishStatus.PROCESSING);
        batchItemRepository.save(item);

        request.setPublishStatus(SocialPostPublishStatus.PROCESSING);
        requestRepository.save(request);
    }

    private void markPublished(SocialPostBatchItem item, VehicleSocialPostRequest request,
                                FacebookGraphClient.FacebookPublishResult result, String caption) {

        item.setStatus(SocialPostPublishStatus.PUBLISHED);
        item.setErrorMessage(null);
        batchItemRepository.save(item);

        request.setPublishStatus(SocialPostPublishStatus.PUBLISHED);
        request.setFacebookPageId(properties.pageId());
        request.setFacebookPostId(result.facebookPostId());
        request.setFacebookPostUrl(result.facebookPostUrl());
        request.setCaption(caption);
        request.setPublishedAt(LocalDateTime.now());
        request.setLastErrorMessage(null);
        requestRepository.save(request);

        log.info("Published request [{}] -> Facebook post [{}]", request.getId(), result.facebookPostId());

        incrementBatchOutcome(item.getBatch().getId(), true);
    }

    private void markFailedTransient(SocialPostBatchItem item, VehicleSocialPostRequest request, String errorMessage) {

        item.setStatus(SocialPostPublishStatus.FAILED);
        item.setRetryable(true);
        item.setRetryCount(item.getRetryCount() + 1);
        item.setErrorMessage(truncate(errorMessage));
        batchItemRepository.save(item);

        request.setPublishStatus(SocialPostPublishStatus.FAILED);
        request.setRetryCount(item.getRetryCount());
        request.setLastErrorMessage(truncate(errorMessage));
        requestRepository.save(request);

        log.warn("Request [{}] failed (transient, retryCount={}): {}",
                request.getId(), item.getRetryCount(), errorMessage);

        incrementBatchOutcome(item.getBatch().getId(), false);
    }

    private void failPermanently(SocialPostBatchItem item, VehicleSocialPostRequest request, String errorMessage) {

        item.setStatus(SocialPostPublishStatus.FAILED);
        item.setRetryable(false);
        item.setRetryCount(item.getRetryCount() + 1);
        item.setErrorMessage(truncate(errorMessage));
        batchItemRepository.save(item);

        request.setPublishStatus(SocialPostPublishStatus.FAILED);
        request.setLastErrorMessage(truncate(errorMessage));
        requestRepository.save(request);

        log.error("Request [{}] failed permanently (not retryable): {}", request.getId(), errorMessage);

        incrementBatchOutcome(item.getBatch().getId(), false);
    }

    private void incrementBatchOutcome(Long batchId, boolean success) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            if (success) {
                batch.setSuccessCount(batch.getSuccessCount() + 1);
            } else {
                batch.setFailedCount(batch.getFailedCount() + 1);
            }
            batch.setPendingCount(Math.max(0, batch.getPendingCount() - 1));
            batchRepository.save(batch);
        });
    }

    private void finalizeBatch(Long batchId) {
        batchRepository.findById(batchId).ifPresent(batch -> {

            if (batch.getPendingCount() > 0) {
                // Shouldn't normally happen (loop processed every queued item),
                // but leave the batch as PROCESSING rather than guessing.
                return;
            }

            SocialPostBatchStatus finalStatus = resolveFinalStatus(batch);
            batch.setStatus(finalStatus);
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            log.info("Batch [{}] finalized with status [{}] ({} success, {} failed of {})",
                    batchId, finalStatus, batch.getSuccessCount(), batch.getFailedCount(), batch.getTotalCount());
        });
    }

    private SocialPostBatchStatus resolveFinalStatus(SocialPostBatch batch) {
        if (batch.getFailedCount() == 0) {
            return SocialPostBatchStatus.COMPLETED;
        }
        if (batch.getSuccessCount() == 0) {
            return SocialPostBatchStatus.FAILED;
        }
        return SocialPostBatchStatus.PARTIALLY_COMPLETED;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
