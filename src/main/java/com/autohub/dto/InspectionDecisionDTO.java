package com.autohub.dto;

import com.autohub.enums.InspectionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDecisionDTO {

    @NotNull(message = "Status is required (VERIFIED or REJECTED)")
    private InspectionStatus status;

    private String inspectionNotes;

    private Integer overallRating;
}

