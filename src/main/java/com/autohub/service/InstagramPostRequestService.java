package com.autohub.service;

import com.autohub.dto.InstagramDealerVehicleStatusDTO;
import com.autohub.dto.InstagramPostRequestBulkRequestDTO;
import com.autohub.dto.InstagramPostRequestBulkResponseDTO;

import java.util.List;

public interface InstagramPostRequestService {

    /**
     * Dealer requests up to 10 of their own vehicles be published to the
     * Instagram Page. dealerId must come from the authenticated JWT
     * principal - never from the request body.
     */
    InstagramPostRequestBulkResponseDTO requestBulkInstagramPost(
            Long dealerId,
            InstagramPostRequestBulkRequestDTO request);

    /**
     * Powers the dealer dashboard vehicle list: each vehicle plus its
     * current Instagram approval/publish status, and whether it's currently
     * selectable for a new request.
     */
    List<InstagramDealerVehicleStatusDTO> getDealerVehicleStatuses(Long dealerId);
}
