package com.autohub.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatUserResponse {

    private Long id;

    private String name;

    private String role;

    private String chatKey;

    private String lastMessage;

    private LocalDateTime lastMessageAt;

    private Long unreadCount;

    private Boolean online;
}

