package com.autohub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FacebookRejectRequestDTO {

    @NotEmpty(message = "At least one request must be selected")
    private List<Long> requestIds;

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
