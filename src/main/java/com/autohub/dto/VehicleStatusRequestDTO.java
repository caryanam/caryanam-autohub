package com.autohub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleStatusRequestDTO {

    @NotBlank(message = "Vehicle Status is Required : ACTIVE or INACTIVE or FEATURED")
    private String status;
}

