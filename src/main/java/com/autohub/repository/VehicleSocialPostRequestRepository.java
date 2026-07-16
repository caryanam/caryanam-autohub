package com.autohub.repository;

import com.autohub.entity.VehicleSocialPostRequest;
import com.autohub.enums.SocialPostApprovalStatus;
import com.autohub.enums.SocialPostPublishStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleSocialPostRequestRepository extends JpaRepository<VehicleSocialPostRequest, Long> {

    List<VehicleSocialPostRequest> findByDealer_Id(Long dealerId);

    /**
     * Used by FacebookPostRequestService for duplicate-active-request
     * protection: a vehicle cannot be re-requested while it already has a
     * row in one of these "active" states.
     */
    @Query("""
       SELECT r
       FROM VehicleSocialPostRequest r
       WHERE r.vehicle.id = :vehicleId
       AND (
            r.approvalStatus = 'PENDING'
            OR r.publishStatus IN ('QUEUED', 'PROCESSING', 'PUBLISHED')
       )
       """)
    List<VehicleSocialPostRequest> findActiveByVehicleId(@Param("vehicleId") Long vehicleId);

    /**
     * Locks the selected rows for the duration of the bulk-approve
     * transaction ("Lock Requests" step) so two concurrent admin actions
     * cannot approve/publish the same request twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT r
       FROM VehicleSocialPostRequest r
       WHERE r.id IN :ids
       AND r.dealer.id = :dealerId
       """)
    List<VehicleSocialPostRequest> findAllByIdInAndDealerIdForUpdate(
            @Param("ids") List<Long> ids,
            @Param("dealerId") Long dealerId);

    long countByDealer_IdAndApprovalStatus(Long dealerId, SocialPostApprovalStatus approvalStatus);

    long countByDealer_IdAndPublishStatus(Long dealerId, SocialPostPublishStatus publishStatus);

    @Query("SELECT DISTINCT r.dealer.id FROM VehicleSocialPostRequest r")
    List<Long> findDistinctDealerIds();
}
