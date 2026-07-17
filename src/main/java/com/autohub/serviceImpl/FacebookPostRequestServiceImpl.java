package com.autohub.serviceImpl;

import com.autohub.dto.FacebookDealerVehicleStatusDTO;
import com.autohub.dto.FacebookPostRequestBulkRequestDTO;
import com.autohub.dto.FacebookPostRequestBulkResponseDTO;
import com.autohub.dto.FacebookPostRequestItemResultDTO;
import com.autohub.entity.Vehicle;
import com.autohub.entity.VehicleMedia;
import com.autohub.entity.VehicleSocialPostRequest;
import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostPublishStatus;
import com.autohub.enums.VehicleStatus;
import com.autohub.repository.VehicleRepository;
import com.autohub.repository.VehicleSocialPostRequestRepository;
import com.autohub.service.FacebookPostRequestService;
import com.autohub.util.SocialPostVehicleUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookPostRequestServiceImpl implements FacebookPostRequestService {

    private static final Set<VehicleStatus> APPROVED_STATUSES = Set.of(VehicleStatus.ACTIVE, VehicleStatus.FEATURED);

    private final VehicleRepository vehicleRepository;

    private final VehicleSocialPostRequestRepository socialPostRequestRepository;

    @Value("${spring.server.url}")
    private String serverUrl;

    @Override
    @Transactional
    public FacebookPostRequestBulkResponseDTO requestBulkFacebookPost(
            Long dealerId,
            FacebookPostRequestBulkRequestDTO request) {

        List<FacebookPostRequestItemResultDTO> results = new ArrayList<>();
        int accepted = 0;

        for (Long vehicleId : request.getVehicleIds()) {

            FacebookPostRequestItemResultDTO result = evaluateAndCreateRequest(dealerId, vehicleId);
            results.add(result);

            if (result.isAccepted()) {
                accepted++;
            }
        }

        log.info("Dealer [{}] submitted {} Facebook post requests, {} accepted, {} skipped",
                dealerId, request.getVehicleIds().size(), accepted, request.getVehicleIds().size() - accepted);

        return FacebookPostRequestBulkResponseDTO.builder()
                .requestedCount(request.getVehicleIds().size())
                .acceptedCount(accepted)
                .skippedCount(request.getVehicleIds().size() - accepted)
                .results(results)
                .build();
    }

    private FacebookPostRequestItemResultDTO evaluateAndCreateRequest(Long dealerId, Long vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);

        if (vehicle == null) {
            return skipped(vehicleId, "Vehicle not found");
        }

        if (vehicle.getDealer() == null || !dealerId.equals(vehicle.getDealer().getId())) {
            // Never reveal that the vehicle belongs to a different dealer -
            // treat it identically to "not found" to avoid leaking IDs.
            return skipped(vehicleId, "Vehicle not found");
        }

        if (!APPROVED_STATUSES.contains(vehicle.getVehicleStatus())) {
            return skipped(vehicleId, "Vehicle is not active");
        }

        if (!hasMandatoryFields(vehicle)) {
            return skipped(vehicleId, "Vehicle is missing mandatory details");
        }

        if (findPrimaryImage(vehicle) == null) {
            return skipped(vehicleId, "Vehicle has no image");
        }

        List<VehicleSocialPostRequest> active = socialPostRequestRepository.findActiveByVehicleId(vehicleId);

        boolean alreadyPublished = active.stream()
                .anyMatch(r -> r.getPublishStatus() == SocialPostPublishStatus.PUBLISHED);
        if (alreadyPublished) {
            return skipped(vehicleId, "Vehicle already published to Facebook");
        }

        boolean alreadyInFlight = active.stream()
                .anyMatch(r -> r.getApprovalStatus() == SocialPostApprovalStatus.PENDING
                        || r.getPublishStatus() == SocialPostPublishStatus.QUEUED
                        || r.getPublishStatus() == SocialPostPublishStatus.PROCESSING);
        if (alreadyInFlight) {
            return skipped(vehicleId, "A request for this vehicle is already pending or in progress");
        }

        VehicleSocialPostRequest saved = socialPostRequestRepository.save(
                VehicleSocialPostRequest.builder()
                        .vehicle(vehicle)
                        .dealer(vehicle.getDealer())
                        .approvalStatus(SocialPostApprovalStatus.PENDING)
                        .publishStatus(SocialPostPublishStatus.NOT_STARTED)
                        .retryCount(0)
                        .build()
        );

        return FacebookPostRequestItemResultDTO.builder()
                .vehicleId(vehicleId)
                .requestId(saved.getId())
                .accepted(true)
                .build();
    }

    private boolean hasMandatoryFields(Vehicle vehicle) {
        return vehicle.getBrand() != null && !vehicle.getBrand().isBlank()
                && vehicle.getModel() != null && !vehicle.getModel().isBlank()
                && vehicle.getVariant() != null && !vehicle.getVariant().isBlank()
                && vehicle.getAskingPrice() != null
                && vehicle.getRegistrationYear() != null
                && vehicle.getFuelType() != null && !vehicle.getFuelType().isBlank()
                && vehicle.getCity() != null && !vehicle.getCity().isBlank();
    }

    private VehicleMedia findPrimaryImage(Vehicle vehicle) {
        return SocialPostVehicleUtil.findPrimaryImage(vehicle);
    }

    private FacebookPostRequestItemResultDTO skipped(Long vehicleId, String reason) {
        return FacebookPostRequestItemResultDTO.builder()
                .vehicleId(vehicleId)
                .accepted(false)
                .reason(reason)
                .build();
    }

    @Override
    public List<FacebookDealerVehicleStatusDTO> getDealerVehicleStatuses(Long dealerId) {

        List<Vehicle> vehicles = vehicleRepository.findByDealerId(dealerId);

        return vehicles.stream()
                .map(this::toDealerVehicleStatusDTO)
                .toList();
    }

    private FacebookDealerVehicleStatusDTO toDealerVehicleStatusDTO(Vehicle vehicle) {

        VehicleSocialPostRequest latest = socialPostRequestRepository
                .findTopByVehicle_IdOrderByCreatedAtDesc(vehicle.getId())
                .orElse(null);

        VehicleMedia primaryImage = findPrimaryImage(vehicle);

        boolean selectable = latest == null
                || (latest.getApprovalStatus() != SocialPostApprovalStatus.PENDING
                && latest.getPublishStatus() != SocialPostPublishStatus.QUEUED
                && latest.getPublishStatus() != SocialPostPublishStatus.PROCESSING
                && latest.getPublishStatus() != SocialPostPublishStatus.PUBLISHED);

        return FacebookDealerVehicleStatusDTO.builder()
                .vehicleId(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .variant(vehicle.getVariant())
                .registrationYear(vehicle.getRegistrationYear())
                .askingPrice(vehicle.getAskingPrice() == null ? null : java.math.BigDecimal.valueOf(vehicle.getAskingPrice()))
                .primaryImageUrl(primaryImage == null ? null : buildPublicUrl(primaryImage))
                .requestId(latest == null ? null : latest.getId())
                .approvalStatus(latest == null ? null : latest.getApprovalStatus())
                .publishStatus(latest == null ? null : latest.getPublishStatus())
                .facebookPostUrl(latest == null ? null : latest.getFacebookPostUrl())
                .rejectionReason(latest == null ? null : latest.getRejectionReason())
                .selectable(selectable)
                .build();
    }

    private String buildPublicUrl(VehicleMedia media) {
        return SocialPostVehicleUtil.buildImageUrl(serverUrl, media);
    }
}
