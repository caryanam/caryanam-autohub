package com.autohub.service;

import com.autohub.dto.ChatMessageRequest;
import com.autohub.dto.ChatUserResponse;
import com.autohub.entity.ChatMessage;

import java.util.List;

public interface ChatService {
    void sendMessage(Long senderId,
                     String senderRole,
                     ChatMessageRequest request);

    List<ChatMessage> getHistory(String roomId);

    String generateRoomId(
            Long senderId,
            String senderRole,
            Long receiverId,
            String receiverRole
    );

    List<ChatUserResponse> getAvailableUsers(
            Long userId,
            String role
    );

    Long getUnreadCount(
            Long userId,
            String role
    );


    void markAsRead(
            String roomId,
            Long receiverId,
            String receiverRole
    );

    void sendGroupMessage(
            Long senderId,
            String senderRole,
            String content
    );

    List<ChatMessage> getGroupHistory();

    void sendGroupMessage(
            Long senderId,
            String senderRole,
            ChatMessageRequest request
    );

    List<ChatMessage> getGroupHistory(
            String groupId
    );



}

