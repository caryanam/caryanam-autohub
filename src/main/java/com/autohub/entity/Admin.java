
package com.autohub.entity;
import com.autohub.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "adminReg")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;

    private String fullName;

    private String mobile;

    private String city;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String otp;

    private LocalDateTime otpGeneratedTime;

    private Boolean isOtpVerified = false;

    @Enumerated(EnumType.STRING)
    private Role role;

}