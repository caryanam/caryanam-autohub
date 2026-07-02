package com.autohub.controller;

import com.autohub.dto.*;
import com.autohub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =====================================================
    // LOGIN API
    // =====================================================
    //ALL
    @PostMapping("/login")
    @Operation(summary = "Login API for All")
    public ResponseEntity<ResponseDto<LoginResponseDTO>> login(
            @RequestBody LoginRequestDTO request) {

        System.out.println("Mobile : " + request.getUsername());

        // REQUEST BODY VALIDATION
        if (request == null) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Request Body is Missing",
                            null
                    ));
        }

        if (request.getUsername() == null
                || request.getUsername().trim().isEmpty()) {
            if (!request.getUsername()
                    .matches("^[6-9]\\d{9}$")) {

                return ResponseEntity.badRequest()
                        .body(new ResponseDto<>(
                                400,
                                "Invalid Mobile Number",
                                null
                        ));
            }
        }

        // USERNAME VALIDATION
        if (request.getUsername() == null
                || request.getUsername().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Email, Dealer Mobile or Executive Mobile is Required",
                            null
                    ));
        }

        // USERNAME LENGTH VALIDATION
        if (request.getUsername().length() > 50) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Username must be maximum 50 characters",
                            null
                    ));
        }

        String username = request.getUsername().trim();

        boolean isEmail = username.contains("@");

        // EMAIL FORMAT VALIDATION
        if (isEmail &&
                !username.matches(
                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Invalid Email Format",
                            null
                    ));
        }

        // MOBILE FORMAT VALIDATION
        if (!isEmail &&
                !username.matches("^[0-9]{10}$")) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Mobile Number must be 10 digits",
                            null
                    ));
        }

        // PASSWORD VALIDATION
        if (request.getPassword() == null
                || request.getPassword().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Password is Required",
                            null
                    ));
        }

        // PASSWORD MIN LENGTH
        if (request.getPassword().length() < 6) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Password must be minimum 6 characters",
                            null
                    ));
        }

        // PASSWORD MAX LENGTH
        if (request.getPassword().length() > 20) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Password must be maximum 20 characters",
                            null
                    ));
        }

        // LOGIN SERVICE CALL
        LoginResponseDTO response =
                authService.login(request);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "Login Successfully",
                        response
                )
        );
    }

    // =====================================================
    // LOGOUT API
    // =====================================================
    @PostMapping("/logout")
    @Operation(summary = "Logout API for All")
    public ResponseEntity<ResponseDto<String>> logout(
            HttpServletRequest request) {

        String authHeader =
                request.getHeader("Authorization");

        // TOKEN VALIDATION
        if (authHeader == null
                || authHeader.trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Authorization Token is Missing",
                            null
                    ));
        }

        // BEARER TOKEN VALIDATION
        if (!authHeader.startsWith("Bearer ")) {

            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(
                            400,
                            "Invalid Authorization Format",
                            null
                    ));
        }

        authService.logout(authHeader);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        200,
                        "Logout Successfully",
                        null
                )
        );
    }

    // =====================================================
    // FORGOT PASSWORD FOR ADMIN, DEALER, CUSTOMER API
    // =====================================================

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(
            @RequestParam String email) {

        return ResponseEntity.ok(
                authService.sendOtp(email));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @RequestBody VerifyOtpDTO dto) {

        return ResponseEntity.ok(
                authService.verifyOtp(dto));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordDTO dto) {

        return ResponseEntity.ok(
                authService.resetPassword(dto));
    }
}