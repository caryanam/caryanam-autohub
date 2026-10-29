package com.autohub.dto;

import com.autohub.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectorResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private String mobile;
    private String assignedArea;
    private Boolean isActive;
    private Role role;
    private Long createdByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long totalCompletedInspections;
    private long totalPendingInspections;
}

