package com.autohub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerLeadStatusRequestDTO {

    @NotBlank(message = "Lead Status is Required : NEW or CONTACTED or CONVERTED")
    private String status;
}

