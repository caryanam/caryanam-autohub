package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacebookPostRequestItemResultDTO {

    private Long vehicleId;

    private Long requestId;

    private boolean accepted;

    /**
     * Populated only when accepted = false, e.g.
     * "Vehicle already published", "Pending request already exists",
     * "Primary image missing", "Vehicle is not active".
     */
    private String reason;
}
