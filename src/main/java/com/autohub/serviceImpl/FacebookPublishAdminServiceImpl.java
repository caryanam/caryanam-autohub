package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.entity.*;
import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostBatchStatus;
import com.autohub.enums.SocialPostPublishStatus;
import com.autohub.exception.BadRequestException;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.SocialPostBatchItemRepository;
import com.autohub.repository.SocialPostBatchRepository;
import com.autohub.repository.VehicleSocialPostRequestRepository;
import com.autohub.service.FacebookPublishAdminService;
import com.autohub.service.FacebookPublishWorkerService;
import com.autohub.util.SocialPostVehicleUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookPublishAdminServiceImpl implements FacebookPublishAdminService {

    // Maximum number of vehicles an admin can approve-and-publish in one
    // batch. Enforced here too (defense in depth) on top of the DTO
    // @Size(max = 10) validation. See FACEBOOK_BATCH_LIMIT in Phase 8 config.
    @Value("${facebook.batch-limit:10}")
    private int batchLimit;

    // Facebook allows a maximum of 3 retry attempts per the retry policy.
    @Value("${facebook.max-retry-count:3}")
    private int maxRetryCount;

    private final VehicleSocialPostRequestRepository socialPostRequestRepository;

    private final SocialPostBatchRepository socialPostBatchRepository;

    private final SocialPostBatchItemRepository socialPostBatchItemRepository;

    private final DealerRepository dealerRepository;

    private final FacebookPublishWorkerService workerService;

    // ================= DEALER SUMMARY =================

    @Override
    public List<FacebookAdminDealerSummaryDTO> getDealerSummaries() {

        List<Long> dealerIds = socialPostRequestRepository.findDistinctDealerIds();

        Map<Long, Dealer> dealersById = dealerRepository.findAllById(dealerIds).stream()
                .collect(Collectors.toMap(Dealer::getId, d -> d));

        return dealerIds.stream()
                .map(dealerId -> buildDealerSummary(dealerId, dealersById.get(dealerId)))
                .toList();
    }

    private FacebookAdminDealerSummaryDTO buildDealerSummary(Long dealerId, Dealer dealer) {
        return FacebookAdminDealerSummaryDTO.builder()
                .dealerId(dealerId)
                .dealerBusinessName(dealer == null ? "Unknown Dealer" : dealer.getBusinessName())
                .pendingCount(socialPostRequestRepository.countByDealer_IdAndApprovalStatus(
                        dealerId, SocialPostApprovalStatus.PENDING))
                .processingCount(socialPostRequestRepository.countByDealer_IdAndPublishStatus(
                        dealerId, SocialPostPublishStatus.PROCESSING))
                .publishedCount(socialPostRequestRepository.countByDealer_IdAndPublishStatus(
                        dealerId, SocialPostPublishStatus.PUBLISHED))
                .failedCount(socialPostRequestRepository.countByDealer_IdAndPublishStatus(
                        dealerId, SocialPostPublishStatus.FAILED))
                .build();
    }

    // ================= DEALER REQUEST DETAIL =================

    @Override
    public List<FacebookAdminVehicleRequestDTO> getDealerRequests(Long dealerId) {

        return socialPostRequestRepository.findByDealer_Id(dealerId).stream()
                .map(this::toAdminVehicleRequestDTO)
                .toList();
    }

    private FacebookAdminVehicleRequestDTO toAdminVehicleRequestDTO(VehicleSocialPostRequest request) {

        Vehicle vehicle = request.getVehicle();

        VehicleMedia primaryImage = SocialPostVehicleUtil.findPrimaryImage(vehicle);
        String primaryImageUrl = primaryImage == null ? null : primaryImage.getFilePath();

        return FacebookAdminVehicleRequestDTO.builder()
                .requestId(request.getId())
                .vehicleId(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .variant(vehicle.getVariant())
                .primaryImageUrl(primaryImageUrl)
                .askingPrice(vehicle.getAskingPrice() == null ? null : java.math.BigDecimal.valueOf(vehicle.getAskingPrice()))
                .fuelType(vehicle.getFuelType())
                .registrationYear(vehicle.getRegistrationYear())
                .approvalStatus(request.getApprovalStatus())
                .publishStatus(request.getPublishStatus())
                .requestedAt(request.getCreatedAt())
                .build();
    }

    // ================= REJECT =================

    @Override
    @Transactional
    public void rejectRequests(FacebookRejectRequestDTO request, Long adminId) {

        List<VehicleSocialPostRequest> requests = socialPostRequestRepository.findAllById(request.getRequestIds());

        for (VehicleSocialPostRequest r : requests) {
            if (r.getApprovalStatus() != SocialPostApprovalStatus.PENDING) {
                throw new BadRequestException(
                        "Request [" + r.getId() + "] is not pending and cannot be rejected");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        for (VehicleSocialPostRequest r : requests) {
            r.setApprovalStatus(SocialPostApprovalStatus.REJECTED);
            r.setRejectionReason(request.getReason());
            r.setApprovedByAdminId(adminId);
            r.setApprovedAt(now);
        }

        socialPostRequestRepository.saveAll(requests);

        log.info("Admin [{}] rejected {} Facebook post requests: {}", adminId, requests.size(), request.getRequestIds());
    }

    // ================= APPROVE & PUBLISH =================

    @Override
    @Transactional
    public FacebookBulkApprovePublishResponseDTO bulkApproveAndPublish(
            FacebookBulkApprovePublishRequestDTO request,
            Long adminId) {

        if (request.getRequestIds().size() > batchLimit) {
            throw new BadRequestException("You can approve a maximum of " + batchLimit + " vehicles at a time");
        }

        // -------- Lock Requests --------
        List<VehicleSocialPostRequest> locked = socialPostRequestRepository
                .findAllByIdInAndDealerIdForUpdate(request.getRequestIds(), request.getDealerId());

        // -------- Validate --------
        if (locked.size() != request.getRequestIds().size()) {
            throw new BadRequestException("One or more requests were not found for this dealer");
        }

        for (VehicleSocialPostRequest r : locked) {
            if (r.getApprovalStatus() != SocialPostApprovalStatus.PENDING) {
                throw new BadRequestException("Request [" + r.getId() + "] is not pending approval");
            }
            if (r.getPublishStatus() == SocialPostPublishStatus.QUEUED
                    || r.getPublishStatus() == SocialPostPublishStatus.PROCESSING
                    || r.getPublishStatus() == SocialPostPublishStatus.PUBLISHED) {
                throw new BadRequestException("Request [" + r.getId() + "] is already queued or published");
            }
        }

        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found: " + request.getDealerId()));

        // -------- Create Batch --------
        SocialPostBatch batch = socialPostBatchRepository.save(
                SocialPostBatch.builder()
                        .dealer(dealer)
                        .approvedByAdminId(adminId)
                        .status(SocialPostBatchStatus.QUEUED)
                        .totalCount(locked.size())
                        .successCount(0)
                        .failedCount(0)
                        .pendingCount(locked.size())
                        .build()
        );

        LocalDateTime now = LocalDateTime.now();

        // -------- Approve + Create Queue --------
        for (VehicleSocialPostRequest r : locked) {
            r.setApprovalStatus(SocialPostApprovalStatus.APPROVED);
            r.setApprovedByAdminId(adminId);
            r.setApprovedAt(now);
            r.setPublishStatus(SocialPostPublishStatus.QUEUED);
        }
        socialPostRequestRepository.saveAll(locked);

        List<SocialPostBatchItem> items = locked.stream()
                .map(r -> SocialPostBatchItem.builder()
                        .batch(batch)
                        .request(r)
                        .vehicle(r.getVehicle())
                        .status(SocialPostPublishStatus.QUEUED)
                        .retryCount(0)
                        .retryable(true)
                        .build())
                .toList();
        socialPostBatchItemRepository.saveAll(items);

        log.info("Admin [{}] approved & queued batch [{}] with {} vehicles for dealer [{}]",
                adminId, batch.getId(), locked.size(), request.getDealerId());

        // -------- Start Worker (only after commit, so it reads committed rows) --------
        triggerWorkerAfterCommit(batch.getId(), false);

        return FacebookBulkApprovePublishResponseDTO.builder()
                .batchId(batch.getId())
                .status(batch.getStatus())
                .totalCount(batch.getTotalCount())
                .build();
    }

    private void triggerWorkerAfterCommit(Long batchId, boolean isRetry) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (isRetry) {
                        workerService.retryBatchAsync(batchId);
                    } else {
                        workerService.processBatchAsync(batchId);
                    }
                }
            });
        } else {
            // No active transaction (e.g. called from a test) - fire immediately.
            if (isRetry) {
                workerService.retryBatchAsync(batchId);
            } else {
                workerService.processBatchAsync(batchId);
            }
        }
    }

    // ================= LIVE STATUS =================

    @Override
    public FacebookBatchStatusDTO getBatchStatus(Long batchId) {

        SocialPostBatch batch = socialPostBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        List<FacebookBatchItemStatusDTO> items = socialPostBatchItemRepository.findByBatch_IdOrderByIdAsc(batchId)
                .stream()
                .map(this::toItemStatusDTO)
                .toList();

        return FacebookBatchStatusDTO.builder()
                .batchId(batch.getId())
                .status(batch.getStatus())
                .totalCount(batch.getTotalCount())
                .successCount(batch.getSuccessCount())
                .failedCount(batch.getFailedCount())
                .pendingCount(batch.getPendingCount())
                .createdAt(batch.getCreatedAt())
                .completedAt(batch.getCompletedAt())
                .items(items)
                .build();
    }

    private FacebookBatchItemStatusDTO toItemStatusDTO(SocialPostBatchItem item) {
        Vehicle vehicle = item.getVehicle();
        return FacebookBatchItemStatusDTO.builder()
                .requestId(item.getRequest().getId())
                .vehicleId(vehicle.getId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getVariant())
                .status(item.getStatus())
                .retryCount(item.getRetryCount())
                .errorMessage(item.getErrorMessage())
                .facebookPostUrl(item.getRequest().getFacebookPostUrl())
                .build();
    }

    // ================= RETRY FAILED =================

    @Override
    @Transactional
    public FacebookBulkApprovePublishResponseDTO retryFailed(FacebookRetryFailedRequestDTO request, Long adminId) {

        SocialPostBatch batch = socialPostBatchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));

        List<SocialPostBatchItem> failedItems = socialPostBatchItemRepository
                .findByBatch_IdAndStatus(request.getBatchId(), SocialPostPublishStatus.FAILED)
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getRetryable())
                        && item.getRetryCount() < maxRetryCount)
                .toList();

        if (failedItems.isEmpty()) {
            throw new BadRequestException("No retryable failed items in this batch");
        }

        for (SocialPostBatchItem item : failedItems) {
            item.setStatus(SocialPostPublishStatus.QUEUED);
            item.setErrorMessage(null);

            VehicleSocialPostRequest r = item.getRequest();
            r.setPublishStatus(SocialPostPublishStatus.RETRY_SCHEDULED);
            r.setLastErrorMessage(null);
        }
        socialPostBatchItemRepository.saveAll(failedItems);
        socialPostRequestRepository.saveAll(
                failedItems.stream().map(SocialPostBatchItem::getRequest).toList());

        batch.setStatus(SocialPostBatchStatus.PROCESSING);
        batch.setFailedCount(batch.getFailedCount() - failedItems.size());
        batch.setPendingCount(batch.getPendingCount() + failedItems.size());
        socialPostBatchRepository.save(batch);

        log.info("Admin [{}] triggered retry for {} failed items in batch [{}]",
                adminId, failedItems.size(), batch.getId());

        triggerWorkerAfterCommit(batch.getId(), true);

        return FacebookBulkApprovePublishResponseDTO.builder()
                .batchId(batch.getId())
                .status(batch.getStatus())
                .totalCount(failedItems.size())
                .build();
    }
}
