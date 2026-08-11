package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramPostRequestBulkResponseDTO {

    private int requestedCount;

    private int acceptedCount;

    private int skippedCount;

    private List<InstagramPostRequestItemResultDTO> results;
}
