package com.autohub.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Data
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String roomId;

    private Long user1Id;

    private String user1Role;

    private Long user2Id;

    private String user2Role;

    private String user1Key;

    private String user2Key;

    private LocalDateTime createdAt =
            LocalDateTime.now();
}