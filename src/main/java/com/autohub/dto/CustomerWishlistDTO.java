package com.autohub.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerWishlistDTO {

    private Long vehicleId;
    private String vehicleName;
    private String brand;
    private Boolean flag;
    private String vehicleImage;
    private Double price;
    private LocalDateTime addedAt;



}
