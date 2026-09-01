package com.autohub.repository;

import com.autohub.entity.VehicleInstagramPostRequest;
import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostPublishStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VehicleInstagramPostRequestRepository extends JpaRepository<VehicleInstagramPostRequest, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM VehicleInstagramPostRequest i WHERE i.vehicle.id = :vehicleId")
    void deleteByVehicleId(@Param("vehicleId") Long vehicleId);

    List<VehicleInstagramPostRequest> findByDealer_Id(Long dealerId);

    /**
     * Used by InstagramPostRequestService for duplicate-active-request
     * protection: a vehicle cannot be re-requested while it already has a
     * row in one of these "active" states.
     */
    @Query("""
       SELECT r
       FROM VehicleInstagramPostRequest r
       WHERE r.vehicle.id = :vehicleId
       AND (
            r.approvalStatus = 'PENDING'
            OR r.publishStatus IN ('QUEUED', 'PROCESSING', 'PUBLISHED')
       )
       """)
    List<VehicleInstagramPostRequest> findActiveByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * Latest request for this vehicle regardless of status - including
     * REJECTED, which findActiveByVehicleId deliberately excludes. Used
     * only for dealer-facing display (e.g. showing a rejection reason),
     * never for "is this vehicle currently blocked" logic.
     */
    java.util.Optional<VehicleInstagramPostRequest> findTopByVehicle_IdOrderByCreatedAtDesc(Long vehicleId);

    java.util.Optional<VehicleInstagramPostRequest> findTopByVehicle_IdAndPublishStatusOrderByCreatedAtDesc(Long vehicleId, SocialPostPublishStatus publishStatus);


    /**
     * Locks the selected rows for the duration of the bulk-approve
     * transaction so two concurrent admin actions cannot approve/publish
     * the same request twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT r
       FROM VehicleInstagramPostRequest r
       WHERE r.id IN :ids
       AND r.dealer.id = :dealerId
       """)
    List<VehicleInstagramPostRequest> findAllByIdInAndDealerIdForUpdate(
            @Param("ids") List<Long> ids,
            @Param("dealerId") Long dealerId);

    long countByDealer_IdAndApprovalStatus(Long dealerId, SocialPostApprovalStatus approvalStatus);

    long countByDealer_IdAndPublishStatus(Long dealerId, SocialPostPublishStatus publishStatus);

    @Query("SELECT DISTINCT r.dealer.id FROM VehicleInstagramPostRequest r")
    List<Long> findDistinctDealerIds();
}
