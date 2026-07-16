package com.autohub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FacebookRetryFailedRequestDTO {

    @NotNull(message = "Batch must be specified")
    private Long batchId;
}
