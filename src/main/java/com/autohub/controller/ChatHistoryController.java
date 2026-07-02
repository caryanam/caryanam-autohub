package com.autohub.controller;
import com.autohub.configuration.CustomUserDetails;
import com.autohub.configuration.OnlineUserStore;
import com.autohub.dto.ChatUserResponse;
import com.autohub.entity.ChatMessage;
import com.autohub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    private final OnlineUserStore onlineUserStore;

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory(
            @RequestParam Long user2Id,
            @RequestParam String user2Role,
            Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal()
                        instanceof CustomUserDetails)) {

            return ResponseEntity.status(401)
                    .build();
        }

        CustomUserDetails user =
                (CustomUserDetails)
                        authentication.getPrincipal();

        Long user1Id = user.getId();

        String user1Role = user.getRole();

        String roomId = chatService.generateRoomId(
                user1Id,
                user1Role,
                user2Id,
                user2Role
        );

        return ResponseEntity.ok(
                chatService.getHistory(roomId)
        );
    }


    @GetMapping("/online")
    public Boolean online(
            @RequestParam Long userId,
            @RequestParam String role){

        String key = role + "_" + userId;

        System.out.println("CHECKING = " + key);
        System.out.println("ONLINE USERS = " + onlineUserStore.getUsers());

        return onlineUserStore.isOnline(role + "_" + userId);
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

        System.out.println(
                "SEEN API CALLED => "
                        + user2Role
                        + "_"
                        + user2Id
        );

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

        System.out.println("SEEN API CALLED");
        System.out.println("ROOM = " + roomId);
        System.out.println("USER = " + user.getRole() + "_" + user.getId());

        chatService.markAsRead(
                roomId,
                user.getId(),
                user.getRole()
        );
    }


    @GetMapping("/users")
    public List<ChatUserResponse> users(
            Authentication authentication){

        CustomUserDetails user =
                (CustomUserDetails)
                        authentication.getPrincipal();

        System.out.println(
                "CURRENT LOGIN USER = "
                        + user.getRole()
                        + " "
                        + user.getId()
        );

        return chatService.getAvailableUsers(
                user.getId(),
                user.getRole()
        );
    }


    @GetMapping("/group/history")
    public ResponseEntity<List<ChatMessage>>
    groupHistory() {

        return ResponseEntity.ok(
                chatService.getGroupHistory(
                        "ALL_DEALERS_GROUP"
                )
        );
    }


}
