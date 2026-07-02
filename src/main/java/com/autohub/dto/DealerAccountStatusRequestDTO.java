package com.autohub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DealerAccountStatusRequestDTO {

    @NotBlank(message = "Dealer Account Status is Required : APPROVED or PENDING")
    private String status;
}
