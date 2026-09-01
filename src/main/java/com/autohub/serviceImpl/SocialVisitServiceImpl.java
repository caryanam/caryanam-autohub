package com.autohub.serviceImpl;

import com.autohub.dto.RecordSocialVisitRequestDTO;
import com.autohub.dto.SocialVisitLogDTO;
import com.autohub.entity.SocialVisitLog;
import com.autohub.entity.Vehicle;
import com.autohub.entity.VehicleInstagramPostRequest;
import com.autohub.entity.VehicleSocialPostRequest;
import com.autohub.enums.SocialPostPublishStatus;
import com.autohub.enums.TrafficSource;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.SocialVisitLogRepository;
import com.autohub.repository.VehicleInstagramPostRequestRepository;
import com.autohub.repository.VehicleRepository;
import com.autohub.repository.VehicleSocialPostRequestRepository;
import com.autohub.service.SocialVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialVisitServiceImpl implements SocialVisitService {

    private final SocialVisitLogRepository visitLogRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleInstagramPostRequestRepository instagramPostRepo;
    private final VehicleSocialPostRequestRepository facebookPostRepo;

    @Override
    public void recordVisit(RecordSocialVisitRequestDTO request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        TrafficSource source;
        try {
            source = TrafficSource.valueOf(request.getSource().toUpperCase());
        } catch (Exception e) {
            log.warn("Invalid traffic source: {}", request.getSource());
            return;
        }

        SocialVisitLog visitLog = new SocialVisitLog();
        visitLog.setVehicle(vehicle);
        visitLog.setDealer(vehicle.getDealer());
        visitLog.setSource(source);
        visitLog.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());

        if (source == TrafficSource.INSTAGRAM) {
            instagramPostRepo.findTopByVehicle_IdAndPublishStatusOrderByCreatedAtDesc(
                    vehicle.getId(), SocialPostPublishStatus.PUBLISHED
            ).ifPresent(post -> {
                visitLog.setPostId(post.getInstagramPostId());
                visitLog.setPostUrl(post.getInstagramPostUrl());
            });
        } else if (source == TrafficSource.FACEBOOK) {
            facebookPostRepo.findTopByVehicle_IdAndPublishStatusOrderByCreatedAtDesc(
                    vehicle.getId(), SocialPostPublishStatus.PUBLISHED
            ).ifPresent(post -> {
                visitLog.setPostId(post.getFacebookPostId());
                visitLog.setPostUrl(post.getFacebookPostUrl());
            });
        }

        visitLogRepository.save(visitLog);
    }

    @Override
    public List<SocialVisitLogDTO> getAllVisits() {
        return visitLogRepository.findAllByOrderByVisitedAtDesc().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SocialVisitLogDTO> getVisitsByVehicle(Long vehicleId) {
        return visitLogRepository.findByVehicle_IdOrderByVisitedAtDesc(vehicleId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SocialVisitLogDTO> getVisitsByDealer(Long dealerId) {
        return visitLogRepository.findByDealer_IdOrderByVisitedAtDesc(dealerId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private SocialVisitLogDTO mapToDTO(SocialVisitLog log) {
        return new SocialVisitLogDTO(
                log.getId(),
                log.getVehicle().getId(),
                log.getVehicleName() != null ? log.getVehicleName() : log.getVehicle().getBrand() + " " + log.getVehicle().getModel(),
                log.getDealer() != null ? log.getDealer().getId() : null,
                log.getDealer() != null ? log.getDealer().getBusinessName() : null,
                log.getSource().name(),
                log.getPostId(),
                log.getPostUrl(),
                log.getVisitedAt()
        );
    }
}
