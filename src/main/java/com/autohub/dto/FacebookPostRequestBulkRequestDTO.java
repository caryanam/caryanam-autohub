package com.autohub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FacebookPostRequestBulkRequestDTO {

    @NotEmpty(message = "At least one vehicle must be selected")
    @Size(max = 10, message = "You can request a maximum of 10 vehicles at a time")
    private List<Long> vehicleIds;
}
