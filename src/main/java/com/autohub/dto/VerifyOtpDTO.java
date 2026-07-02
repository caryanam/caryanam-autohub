package com.autohub.dto;

import lombok.Data;

@Data
public class VerifyOtpDTO {

    private String email;
    private String otp;
}