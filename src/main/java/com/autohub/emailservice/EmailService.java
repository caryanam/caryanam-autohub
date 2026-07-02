package com.autohub.emailservice;

public interface EmailService {


    void sendMail(String to, String subject, String body);

    void sendOtp(String email, String otp);

}