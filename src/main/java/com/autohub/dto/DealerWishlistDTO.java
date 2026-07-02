package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DealerWishlistDTO {

    private Long vehicleId;
    private String vehicleName;

    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private LocalDateTime addedAt;
}
