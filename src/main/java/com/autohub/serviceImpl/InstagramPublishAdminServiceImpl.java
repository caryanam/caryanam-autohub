package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.entity.*;
import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostBatchStatus;
import com.autohub.enums.SocialPostPublishStatus;
import com.autohub.exception.BadRequestException;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.InstagramPublishAdminService;
import com.autohub.service.InstagramPublishWorkerService;
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
public class InstagramPublishAdminServiceImpl implements InstagramPublishAdminService {

    @Value("${instagram.batch-limit:10}")
    private int batchLimit;

    @Value("${instagram.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${spring.server.url}")
    private String serverUrl;

    private final VehicleInstagramPostRequestRepository instagramPostRequestRepository;

    private final InstagramPostBatchRepository instagramPostBatchRepository;

    private final InstagramPostBatchItemRepository instagramPostBatchItemRepository;

    private final DealerRepository dealerRepository;

    private final InstagramPublishWorkerService workerService;

    // ================= DEALER SUMMARY =================

    @Override
    public List<InstagramAdminDealerSummaryDTO> getDealerSummaries() {

        List<Long> dealerIds = instagramPostRequestRepository.findDistinctDealerIds();

        Map<Long, Dealer> dealersById = dealerRepository.findAllById(dealerIds).stream()
                .collect(Collectors.toMap(Dealer::getId, d -> d));

        return dealerIds.stream()
                .map(dealerId -> buildDealerSummary(dealerId, dealersById.get(dealerId)))
                .toList();
    }

    private InstagramAdminDealerSummaryDTO buildDealerSummary(Long dealerId, Dealer dealer) {
        return InstagramAdminDealerSummaryDTO.builder()
                .dealerId(dealerId)
                .dealerBusinessName(dealer == null ? "Unknown Dealer" : dealer.getBusinessName())
                .pendingCount(instagramPostRequestRepository.countByDealer_IdAndApprovalStatus(
                        dealerId, SocialPostApprovalStatus.PENDING))
                .processingCount(instagramPostRequestRepository.countByDealer_IdAndPublishStatus(
                        dealerId, SocialPostPublishStatus.PROCESSING))
                .publishedCount(instagramPostRequestRepository.countByDealer_IdAndPublishStatus(
                        dealerId, SocialPostPublishStatus.PUBLISHED))
                .failedCount(instagramPostRequestRepository.countByDealer_IdAndPublishStatus(
                        dealerId, SocialPostPublishStatus.FAILED))
                .build();
    }

    // ================= DEALER REQUEST DETAIL =================

    @Override
    public List<InstagramAdminVehicleRequestDTO> getDealerRequests(Long dealerId) {

        List<VehicleInstagramPostRequest> allRequests = instagramPostRequestRepository.findByDealer_Id(dealerId);

        // Group by vehicleId and keep only the latest request per vehicle
        Map<Long, VehicleInstagramPostRequest> latestRequests = allRequests.stream()
                .collect(Collectors.toMap(
                        r -> r.getVehicle().getId(),
                        r -> r,
                        (r1, r2) -> r1.getCreatedAt().isAfter(r2.getCreatedAt()) ? r1 : r2
                ));

        return latestRequests.values().stream()
                .map(this::toAdminVehicleRequestDTO)
                .sorted((a, b) -> b.getRequestedAt().compareTo(a.getRequestedAt()))
                .toList();
    }

    private InstagramAdminVehicleRequestDTO toAdminVehicleRequestDTO(VehicleInstagramPostRequest request) {

        Vehicle vehicle = request.getVehicle();

        VehicleMedia primaryImage = SocialPostVehicleUtil.findPrimaryImage(vehicle);
        String primaryImageUrl = primaryImage == null ? null
                : SocialPostVehicleUtil.buildImageUrl(serverUrl, primaryImage);

        return InstagramAdminVehicleRequestDTO.builder()
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
    public void rejectRequests(InstagramRejectRequestDTO request, Long adminId) {

        List<VehicleInstagramPostRequest> requests = instagramPostRequestRepository.findAllById(request.getRequestIds());

        for (VehicleInstagramPostRequest r : requests) {
            if (r.getApprovalStatus() != SocialPostApprovalStatus.PENDING) {
                throw new BadRequestException(
                        "Request [" + r.getId() + "] is not pending and cannot be rejected");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        for (VehicleInstagramPostRequest r : requests) {
            r.setApprovalStatus(SocialPostApprovalStatus.REJECTED);
            r.setRejectionReason(request.getReason());
            r.setApprovedByAdminId(adminId);
            r.setApprovedAt(now);
        }

        instagramPostRequestRepository.saveAll(requests);

        log.info("Admin [{}] rejected {} Instagram post requests: {}", adminId, requests.size(), request.getRequestIds());
    }

    // ================= APPROVE & PUBLISH =================

    @Override
    @Transactional
    public InstagramBulkApprovePublishResponseDTO bulkApproveAndPublish(
            InstagramBulkApprovePublishRequestDTO request,
            Long adminId) {

        if (request.getRequestIds().size() > batchLimit) {
            throw new BadRequestException("You can approve a maximum of " + batchLimit + " vehicles at a time");
        }

        // -------- Lock Requests --------
        List<VehicleInstagramPostRequest> locked = instagramPostRequestRepository
                .findAllByIdInAndDealerIdForUpdate(request.getRequestIds(), request.getDealerId());

        // -------- Validate --------
        if (locked.size() != request.getRequestIds().size()) {
            throw new BadRequestException("One or more requests were not found for this dealer");
        }

        for (VehicleInstagramPostRequest r : locked) {
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
        InstagramPostBatch batch = instagramPostBatchRepository.save(
                InstagramPostBatch.builder()
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
        for (VehicleInstagramPostRequest r : locked) {
            r.setApprovalStatus(SocialPostApprovalStatus.APPROVED);
            r.setApprovedByAdminId(adminId);
            r.setApprovedAt(now);
            r.setPublishStatus(SocialPostPublishStatus.QUEUED);
        }
        instagramPostRequestRepository.saveAll(locked);

        List<InstagramPostBatchItem> items = locked.stream()
                .map(r -> InstagramPostBatchItem.builder()
                        .batch(batch)
                        .request(r)
                        .vehicle(r.getVehicle())
                        .status(SocialPostPublishStatus.QUEUED)
                        .retryCount(0)
                        .retryable(true)
                        .build())
                .toList();
        instagramPostBatchItemRepository.saveAll(items);

        log.info("Admin [{}] approved & queued Instagram batch [{}] with {} vehicles for dealer [{}]",
                adminId, batch.getId(), locked.size(), request.getDealerId());

        // -------- Start Worker (only after commit, so it reads committed rows) --------
        triggerWorkerAfterCommit(batch.getId(), false);

        return InstagramBulkApprovePublishResponseDTO.builder()
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
            if (isRetry) {
                workerService.retryBatchAsync(batchId);
            } else {
                workerService.processBatchAsync(batchId);
            }
        }
    }

    // ================= LIVE STATUS =================

    @Override
    public InstagramBatchStatusDTO getBatchStatus(Long batchId) {

        InstagramPostBatch batch = instagramPostBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        List<InstagramBatchItemStatusDTO> items = instagramPostBatchItemRepository.findByBatch_IdOrderByIdAsc(batchId)
                .stream()
                .map(this::toItemStatusDTO)
                .toList();

        return InstagramBatchStatusDTO.builder()
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

    private InstagramBatchItemStatusDTO toItemStatusDTO(InstagramPostBatchItem item) {
        Vehicle vehicle = item.getVehicle();
        return InstagramBatchItemStatusDTO.builder()
                .requestId(item.getRequest().getId())
                .vehicleId(vehicle.getId())
                .vehicleName(vehicle.getBrand() + " " + vehicle.getModel() + " " + vehicle.getVariant())
                .status(item.getStatus())
                .retryCount(item.getRetryCount())
                .errorMessage(item.getErrorMessage())
                .instagramPostUrl(item.getRequest().getInstagramPostUrl())
                .build();
    }

    // ================= RETRY FAILED =================

    @Override
    @Transactional
    public InstagramBulkApprovePublishResponseDTO retryFailed(InstagramRetryFailedRequestDTO request, Long adminId) {

        InstagramPostBatch batch = instagramPostBatchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));

        List<InstagramPostBatchItem> failedItems = instagramPostBatchItemRepository
                .findByBatch_IdAndStatus(request.getBatchId(), SocialPostPublishStatus.FAILED)
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getRetryable())
                        && item.getRetryCount() < maxRetryCount)
                .toList();

        if (failedItems.isEmpty()) {
            throw new BadRequestException("No retryable failed items in this batch");
        }

        for (InstagramPostBatchItem item : failedItems) {
            item.setStatus(SocialPostPublishStatus.QUEUED);
            item.setErrorMessage(null);

            VehicleInstagramPostRequest r = item.getRequest();
            r.setPublishStatus(SocialPostPublishStatus.RETRY_SCHEDULED);
            r.setLastErrorMessage(null);
        }
        instagramPostBatchItemRepository.saveAll(failedItems);
        instagramPostRequestRepository.saveAll(
                failedItems.stream().map(InstagramPostBatchItem::getRequest).toList());

        batch.setStatus(SocialPostBatchStatus.PROCESSING);
        batch.setFailedCount(batch.getFailedCount() - failedItems.size());
        batch.setPendingCount(batch.getPendingCount() + failedItems.size());
        instagramPostBatchRepository.save(batch);

        log.info("Admin [{}] triggered retry for {} failed items in Instagram batch [{}]",
                adminId, failedItems.size(), batch.getId());

        triggerWorkerAfterCommit(batch.getId(), true);

        return InstagramBulkApprovePublishResponseDTO.builder()
                .batchId(batch.getId())
                .status(batch.getStatus())
                .totalCount(failedItems.size())
                .build();
    }
}
