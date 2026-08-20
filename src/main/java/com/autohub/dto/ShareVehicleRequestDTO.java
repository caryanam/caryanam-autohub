package com.autohub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShareVehicleRequestDTO {

    @NotNull(message = "Dealer ID is required")
    private Long dealerId;

    private boolean shareToSelf = false;

    @Pattern(
        regexp = "^$|^[6-9]\\d{9}$",
        message = "Customer WhatsApp must be a valid 10-digit Indian mobile number"
    )
    private String customerWhatsapp;
}
