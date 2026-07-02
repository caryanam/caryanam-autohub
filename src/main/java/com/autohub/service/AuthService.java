package com.autohub.service;


import com.autohub.dto.LoginRequestDTO;
import com.autohub.dto.LoginResponseDTO;
import com.autohub.dto.ResetPasswordDTO;
import com.autohub.dto.VerifyOtpDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO dto);

    void logout(String token);

    //FORGOT PASS FOR ALL
    String sendOtp(String email);

    String verifyOtp(VerifyOtpDTO dto);

    String resetPassword(ResetPasswordDTO dto);


}
