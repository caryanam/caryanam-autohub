package com.autohub.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;

    private Long senderId;

    private String senderRole;

    private String senderName;

    private Long receiverId;

    private String receiverRole;

    private String senderKey;

    private String receiverKey;

    private Boolean isRead = false;

    private LocalDateTime readAt;

    private Boolean groupMessage = false;

    private String groupId;


    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime sentAt =
            LocalDateTime.now();
}


