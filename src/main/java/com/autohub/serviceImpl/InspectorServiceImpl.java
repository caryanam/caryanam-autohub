package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.entity.*;
import com.autohub.enums.InspectionStatus;
import com.autohub.enums.Role;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.InspectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InspectorServiceImpl implements InspectorService {

    private final InspectorRepository inspectorRepository;
    private final VehicleInspectionRequestRepository inspectionRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final DealerRepository dealerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.server.url:https://c1.caryanam.com}")
    private String serverUrl;

    // ==========================================
    // ADMIN INSPECTOR CRUD
    // ==========================================

    @Override
    @Transactional
    public InspectorResponseDTO createInspector(InspectorRequestDTO request, Long adminId) {
        if (inspectorRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Inspector with this email already exists");
        }
        if (inspectorRepository.existsByMobile(request.getMobile().trim())) {
            throw new RuntimeException("Inspector with this mobile number already exists");
        }

        Inspector inspector = Inspector.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .mobile(request.getMobile().trim())
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .assignedArea(request.getAssignedArea().trim())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .role(Role.INSPECTOR)
                .createdByAdminId(adminId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Inspector saved = inspectorRepository.save(inspector);
        return mapToResponse(saved);
    }

    @Override
    public List<InspectorResponseDTO> getAllInspectors() {
        return inspectorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public InspectorResponseDTO getInspectorById(Long id) {
        Inspector inspector = inspectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + id));
        return mapToResponse(inspector);
    }

    @Override
    @Transactional
    public InspectorResponseDTO updateInspector(Long id, InspectorRequestDTO request) {
        Inspector inspector = inspectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + id));

        // Check email uniqueness if changed
        if (!inspector.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
            if (inspectorRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
                throw new RuntimeException("Email already in use by another inspector");
            }
            inspector.setEmail(request.getEmail().trim().toLowerCase());
        }

        // Check mobile uniqueness if changed
        if (!inspector.getMobile().equals(request.getMobile().trim())) {
            if (inspectorRepository.existsByMobile(request.getMobile().trim())) {
                throw new RuntimeException("Mobile already in use by another inspector");
            }
            inspector.setMobile(request.getMobile().trim());
        }

        inspector.setFullName(request.getFullName().trim());
        inspector.setAssignedArea(request.getAssignedArea().trim());

        if (request.getIsActive() != null) {
            inspector.setIsActive(request.getIsActive());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            inspector.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        inspector.setUpdatedAt(LocalDateTime.now());
        Inspector saved = inspectorRepository.save(inspector);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteInspector(Long id) {
        Inspector inspector = inspectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + id));
        // Soft deactivate or delete
        inspector.setIsActive(false);
        inspectorRepository.save(inspector);
    }

    @Override
    @Transactional
    public void toggleInspectorStatus(Long id, boolean active) {
        Inspector inspector = inspectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + id));
        inspector.setIsActive(active);
        inspector.setUpdatedAt(LocalDateTime.now());
        inspectorRepository.save(inspector);
    }

    // ==========================================
    // INSPECTOR PORTAL OPERATIONS
    // ==========================================

    @Override
    public InspectorResponseDTO getInspectorProfile(Long inspectorId) {
        return getInspectorById(inspectorId);
    }

    @Override
    public List<VehicleInspectionRequestResponseDTO> getPendingRequestsForInspector(Long inspectorId) {
        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + inspectorId));

        String area = inspector.getAssignedArea();
        List<VehicleInspectionRequest> requests = inspectionRequestRepository.findPendingForInspectorOrArea(
                area, inspectorId, InspectionStatus.PENDING
        );

        return requests.stream().map(this::mapToRequestResponse).toList();
    }

    @Override
    public List<VehicleInspectionRequestResponseDTO> getCompletedRequestsForInspector(Long inspectorId) {
        List<VehicleInspectionRequest> requests = inspectionRequestRepository.findByInspectorId(inspectorId);
        return requests.stream()
                .filter(r -> r.getStatus() == InspectionStatus.VERIFIED || r.getStatus() == InspectionStatus.REJECTED)
                .map(this::mapToRequestResponse)
                .toList();
    }

    @Override
    public VehicleInspectionRequestResponseDTO getInspectionRequestById(Long requestId) {
        VehicleInspectionRequest request = inspectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection request not found with id: " + requestId));
        return mapToRequestResponse(request);
    }

    @Override
    @Transactional
    public VehicleInspectionRequestResponseDTO verifyVehicle(Long requestId, Long inspectorId, InspectionDecisionDTO decision) {
        VehicleInspectionRequest request = inspectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection request not found with id: " + requestId));

        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + inspectorId));

        LocalDateTime now = LocalDateTime.now();

        // 1. Update Request
        request.setInspector(inspector);
        request.setStatus(InspectionStatus.VERIFIED);
        request.setCompletedDate(now);
        request.setInspectionNotes(decision != null ? decision.getInspectionNotes() : "Physical inspection completed and certified.");
        if (decision != null && decision.getOverallRating() != null) {
            request.setOverallRating(decision.getOverallRating());
        }
        VehicleInspectionRequest savedRequest = inspectionRequestRepository.save(request);

        // 2. Update Vehicle
        Vehicle vehicle = request.getVehicle();
        if (vehicle != null) {
            vehicle.setIsCertified(true);
            vehicle.setCertifyRequested(true);
            vehicle.setInspectionStatus(InspectionStatus.VERIFIED);
            vehicle.setCertifiedAt(now);
            vehicle.setCertifiedBy(inspector);
            vehicle.setInspectionNotes(request.getInspectionNotes());
            vehicleRepository.save(vehicle);
        }

        return mapToRequestResponse(savedRequest);
    }

    @Override
    @Transactional
    public VehicleInspectionRequestResponseDTO rejectVehicle(Long requestId, Long inspectorId, InspectionDecisionDTO decision) {
        VehicleInspectionRequest request = inspectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection request not found with id: " + requestId));

        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + inspectorId));

        LocalDateTime now = LocalDateTime.now();

        // 1. Update Request
        request.setInspector(inspector);
        request.setStatus(InspectionStatus.REJECTED);
        request.setCompletedDate(now);
        request.setInspectionNotes(decision != null ? decision.getInspectionNotes() : "Vehicle failed physical inspection criteria.");
        if (decision != null && decision.getOverallRating() != null) {
            request.setOverallRating(decision.getOverallRating());
        }
        VehicleInspectionRequest savedRequest = inspectionRequestRepository.save(request);

        // 2. Update Vehicle: Remains active as uncertified
        Vehicle vehicle = request.getVehicle();
        if (vehicle != null) {
            vehicle.setIsCertified(false);
            vehicle.setInspectionStatus(InspectionStatus.REJECTED);
            vehicle.setInspectionNotes(request.getInspectionNotes());
            vehicleRepository.save(vehicle);
        }

        return mapToRequestResponse(savedRequest);
    }

    // ==========================================
    // ADMIN OVERSIGHT & REPORTS
    // ==========================================

    @Override
    public List<VehicleInspectionRequestResponseDTO> getAllInspectionReports(
            InspectionStatus status,
            String area,
            Long dealerId,
            Long inspectorId
    ) {
        List<VehicleInspectionRequest> all = inspectionRequestRepository.findAllByOrderByRequestDateDesc();

        return all.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> area == null || area.isBlank() || r.getArea().equalsIgnoreCase(area.trim()))
                .filter(r -> dealerId == null || (r.getDealer() != null && r.getDealer().getId().equals(dealerId)))
                .filter(r -> inspectorId == null || (r.getInspector() != null && r.getInspector().getId().equals(inspectorId)))
                .map(this::mapToRequestResponse)
                .toList();
    }

    @Override
    public InspectionStatsDTO getInspectionStats() {
        long totalRequests = inspectionRequestRepository.count();
        long pending = inspectionRequestRepository.countByStatus(InspectionStatus.PENDING);
        long verified = inspectionRequestRepository.countByStatus(InspectionStatus.VERIFIED);
        long rejected = inspectionRequestRepository.countByStatus(InspectionStatus.REJECTED);
        long totalInspectors = inspectorRepository.count();
        long activeInspectors = inspectorRepository.countByIsActiveTrue();

        return InspectionStatsDTO.builder()
                .totalRequests(totalRequests)
                .pendingRequests(pending)
                .verifiedCount(verified)
                .rejectedCount(rejected)
                .totalInspectors(totalInspectors)
                .activeInspectors(activeInspectors)
                .build();
    }

    @Override
    @Transactional
    public VehicleInspectionRequestResponseDTO assignInspector(Long requestId, Long inspectorId) {
        VehicleInspectionRequest request = inspectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection request not found with id: " + requestId));

        Inspector inspector = inspectorRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector not found with id: " + inspectorId));

        request.setInspector(inspector);
        VehicleInspectionRequest saved = inspectionRequestRepository.save(request);
        return mapToRequestResponse(saved);
    }

    // ==========================================
    // DEALER CERTIFICATION HOOK
    // ==========================================

    @Override
    @Transactional
    public void createOrUpdateInspectionRequestForVehicle(Long vehicleId, Long dealerId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + dealerId));

        // Determine area matching: priority to vehicle city, fallback to dealer city/address
        String area = vehicle.getCity() != null && !vehicle.getCity().isBlank()
                ? vehicle.getCity().trim()
                : (dealer.getCity() != null ? dealer.getCity().trim() : "Default");

        Optional<VehicleInspectionRequest> existingOpt =
                inspectionRequestRepository.findFirstByVehicleIdOrderByRequestDateDesc(vehicleId);

        if (existingOpt.isPresent()) {
            VehicleInspectionRequest existing = existingOpt.get();
            // If already verified, do nothing
            if (existing.getStatus() == InspectionStatus.VERIFIED) {
                return;
            }
            existing.setStatus(InspectionStatus.PENDING);
            existing.setArea(area);
            existing.setRequestDate(LocalDateTime.now());
            inspectionRequestRepository.save(existing);
        } else {
            VehicleInspectionRequest newRequest = VehicleInspectionRequest.builder()
                    .vehicle(vehicle)
                    .dealer(dealer)
                    .area(area)
                    .status(InspectionStatus.PENDING)
                    .requestDate(LocalDateTime.now())
                    .build();
            inspectionRequestRepository.save(newRequest);
        }
    }

    // ==========================================
    // MAPPERS & HELPERS
    // ==========================================

    private InspectorResponseDTO mapToResponse(Inspector inspector) {
        long completed = inspectionRequestRepository.findByInspectorIdAndStatus(inspector.getId(), InspectionStatus.VERIFIED).size();
        long pending = inspectionRequestRepository.findByAreaIgnoreCaseAndStatus(inspector.getAssignedArea(), InspectionStatus.PENDING).size();

        return InspectorResponseDTO.builder()
                .id(inspector.getId())
                .fullName(inspector.getFullName())
                .email(inspector.getEmail())
                .mobile(inspector.getMobile())
                .assignedArea(inspector.getAssignedArea())
                .isActive(inspector.getIsActive())
                .role(inspector.getRole())
                .createdByAdminId(inspector.getCreatedByAdminId())
                .createdAt(inspector.getCreatedAt())
                .updatedAt(inspector.getUpdatedAt())
                .totalCompletedInspections(completed)
                .totalPendingInspections(pending)
                .build();
    }

    private VehicleInspectionRequestResponseDTO mapToRequestResponse(VehicleInspectionRequest req) {
        Vehicle v = req.getVehicle();
        Dealer d = req.getDealer();
        Inspector i = req.getInspector();

        List<String> images = new ArrayList<>();
        String thumbnail = null;

        if (v != null && v.getMediaList() != null && !v.getMediaList().isEmpty()) {
            images = v.getMediaList().stream()
                    .filter(m -> "IMAGE".equalsIgnoreCase(m.getMediaType()))
                    .map(m -> buildMediaUrl(m.getFilePath()))
                    .toList();
            if (!images.isEmpty()) {
                thumbnail = images.get(0);
            }
        }

        return VehicleInspectionRequestResponseDTO.builder()
                .id(req.getId())
                .vehicleId(v != null ? v.getId() : null)
                .vehicleBrand(v != null ? v.getBrand() : "")
                .vehicleModel(v != null ? v.getModel() : "")
                .vehicleVariant(v != null ? v.getVariant() : "")
                .registrationYear(v != null ? v.getRegistrationYear() : null)
                .askingPrice(v != null && v.getAskingPrice() != null ? BigDecimal.valueOf(v.getAskingPrice()) : null)
                .kilometerDriven(v != null ? v.getKilometerDriven() : null)
                .fuelType(v != null ? v.getFuelType() : "")
                .vehicleCity(v != null ? v.getCity() : "")
                .vehicleType(v != null && v.getVehicleType() != null ? v.getVehicleType().name() : "")
                .vehicleThumbnail(thumbnail)
                .vehicleImages(images)

                .dealerId(d != null ? d.getId() : null)
                .dealerBusinessName(d != null ? d.getBusinessName() : "")
                .dealerOwnerName(d != null ? d.getOwnerName() : "")
                .dealerMobile(d != null ? d.getDealerMobile() : "")
                .dealerWhatsapp(d != null ? d.getWhatsapp() : "")
                .dealerEmail(d != null ? d.getEmail() : "")
                .dealerAddress(d != null ? d.getAddress() : "")
                .dealerCity(d != null ? d.getCity() : "")

                .inspectorId(i != null ? i.getId() : null)
                .inspectorName(i != null ? i.getFullName() : null)
                .inspectorMobile(i != null ? i.getMobile() : null)
                .inspectorEmail(i != null ? i.getEmail() : null)
                .inspectorAssignedArea(i != null ? i.getAssignedArea() : null)

                .area(req.getArea())
                .status(req.getStatus())
                .requestDate(req.getRequestDate())
                .completedDate(req.getCompletedDate())
                .inspectionNotes(req.getInspectionNotes())
                .overallRating(req.getOverallRating())
                .isCertified(v != null && Boolean.TRUE.equals(v.getIsCertified()))
                .build();
    }

    private String buildMediaUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        String normalized = storedPath.replace("\\", "/").trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return serverUrl + normalized;
    }
}

