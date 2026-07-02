package com.autohub.dto;

import lombok.Data;

@Data
public class TypingDTO {

    private Long senderId;

    private String senderRole;

    private Long receiverId;

    private String receiverRole;

    private Boolean typing;
}
