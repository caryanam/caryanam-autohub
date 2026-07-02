package com.autohub.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {

    private String roomId;

    private Long senderId;

    private String senderRole;

    private String senderName;

    private Long receiverId;

    private String receiverRole;

    private String content;

    private Boolean isRead;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;



}

