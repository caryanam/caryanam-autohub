package com.autohub.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSidebarDTO {

    private String roomId;

    private Long userId;

    private String role;

    private String name;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private Long unreadCount;

    private Boolean online;
}
