package com.autohub.dto;

import com.autohub.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegistrationResponseDTO {

    private Long id;

    private String customerName;

    private String customerMobile;

    private String customerCity;

    private String customerEmail;

    private String customerPassword;

    private LocalDateTime accountCreatedAt;

    private Role customerRole;

}
