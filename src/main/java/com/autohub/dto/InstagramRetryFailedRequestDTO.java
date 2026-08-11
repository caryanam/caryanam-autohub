package com.autohub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstagramRetryFailedRequestDTO {

    @NotNull(message = "Batch must be specified")
    private Long batchId;
}
