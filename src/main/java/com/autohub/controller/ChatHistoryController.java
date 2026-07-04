package com.autohub.controller;

import com.autohub.configuration.CustomUserDetails;
import com.autohub.dto.ChatUserResponse;
import com.autohub.entity.ChatMessage;
import com.autohub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping("/users")
    public List<ChatUserResponse> users(
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails)
                        authentication.getPrincipal();

        return chatService.getAvailableUsers(
                user.getId(),
                user.getRole()
        );
    }

    @GetMapping("/history")
    public List<ChatMessage> history(
            @RequestParam Long user2Id,
            @RequestParam String user2Role,
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails)
                        authentication.getPrincipal();

        String roomId =
                chatService.generateRoomId(
                        user.getId(),
                        user.getRole(),
                        user2Id,
                        user2Role
                );

        return chatService.getHistory(
                roomId
        );
    }

    @GetMapping("/group/history")
    public List<ChatMessage> groupHistory(){

        return chatService.getGroupHistory(
                "ALL_DEALERS_GROUP"
        );
    }

    @GetMapping("/unread-count")
    public Long unreadCount(
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails)
                        authentication.getPrincipal();

        return chatService.getUnreadCount(
                user.getId(),
                user.getRole()
        );
    }

    @PostMapping("/seen")
    public void seen(
            @RequestParam Long user2Id,
            @RequestParam String user2Role,
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails)
                        authentication.getPrincipal();

        String roomId =
                chatService.generateRoomId(
                        user.getId(),
                        user.getRole(),
                        user2Id,
                        user2Role
                );

        chatService.markAsRead(
                roomId,
                user.getId(),
                user.getRole()
        );
    }
}


