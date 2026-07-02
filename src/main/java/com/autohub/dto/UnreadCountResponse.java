package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnreadCountResponse {

    private Long senderId;

    private String senderRole;

    private Long count;
}