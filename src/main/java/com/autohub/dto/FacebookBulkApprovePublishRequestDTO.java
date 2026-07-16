package com.autohub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FacebookBulkApprovePublishRequestDTO {

    @NotNull(message = "Dealer must be selected")
    private Long dealerId;

    @NotEmpty(message = "At least one request must be selected")
    @Size(max = 10, message = "You can approve a maximum of 10 vehicles at a time")
    private List<Long> requestIds;
}
