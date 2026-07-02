package com.autohub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {

    private Long userId;

    private String applicationId;

    private String fullName;

    private String email;

    private String mobileNumber;

    // DEALER or INDIVIDUAL
    private String registrationType;

    private String fileName;
    private String fileType;
    private String filePath;
    private String mediaType;

    // Dealer Code
    //private String dealerCode;

    private String role;

    private LocalDateTime createdAt;

}