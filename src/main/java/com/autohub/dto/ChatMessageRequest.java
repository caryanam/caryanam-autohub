package com.autohub.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {

    private Long receiverId;

    private String receiverRole;

    private String content;

    private Boolean groupMessage = false;

    private String groupId;
}