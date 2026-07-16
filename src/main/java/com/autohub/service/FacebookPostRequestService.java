package com.autohub.service;

import com.autohub.dto.FacebookDealerVehicleStatusDTO;
import com.autohub.dto.FacebookPostRequestBulkRequestDTO;
import com.autohub.dto.FacebookPostRequestBulkResponseDTO;

import java.util.List;

public interface FacebookPostRequestService {

    /**
     * Dealer requests up to 10 of their own vehicles be published to the
     * Facebook Page. dealerId must come from the authenticated JWT
     * principal - never from the request body.
     */
    FacebookPostRequestBulkResponseDTO requestBulkFacebookPost(
            Long dealerId,
            FacebookPostRequestBulkRequestDTO request);

    /**
     * Powers the dealer dashboard vehicle list: each vehicle plus its
     * current Facebook approval/publish status, and whether it's currently
     * selectable for a new request.
     */
    List<FacebookDealerVehicleStatusDTO> getDealerVehicleStatuses(Long dealerId);
}
