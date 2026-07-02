package com.autohub.controller;

import com.autohub.configuration.CustomUserDetails;
import com.autohub.dto.ChatMessageRequest;
import com.autohub.dto.GroupMessageRequest;
import com.autohub.dto.TypingDTO;
import com.autohub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/chat.send")
    public void sendMessage(
            ChatMessageRequest request,
            SimpMessageHeaderAccessor accessor) {

        Long userId =
                (Long) accessor
                        .getSessionAttributes()
                        .get("userId");

        String role =
                (String) accessor
                        .getSessionAttributes()
                        .get("role");

        if (Boolean.TRUE.equals(
                request.getGroupMessage())) {

            chatService.sendGroupMessage(
                    userId,
                    role,
                    request
            );

            return;
        }

        chatService.sendMessage(
                userId,
                role,
                request
        );
    }



//    @MessageMapping("/chat.send")
//    public void sendMessage(
//            ChatMessageRequest request,
//            SimpMessageHeaderAccessor accessor) {
//
//        Long loggedInUserId =
//                (Long) accessor
//                        .getSessionAttributes()
//                        .get("userId");
//
//        String loggedInRole =
//                (String) accessor
//                        .getSessionAttributes()
//                        .get("role");
//
//        chatService.sendMessage(
//                loggedInUserId,
//                loggedInRole,
//                request
//        );
//    }


    @MessageMapping("/chat.typing")
    public void typing(
            TypingDTO request){

        messagingTemplate.convertAndSend(
                "/queue/"
                        + request.getReceiverRole()
                        + "_"
                        + request.getReceiverId()
                        + "_typing",
                request
        );
    }

    @MessageMapping("/group.send")
    public void sendGroupMessage(
            GroupMessageRequest request,
            SimpMessageHeaderAccessor accessor) {

        Long userId =
                (Long) accessor
                        .getSessionAttributes()
                        .get("userId");

        String role =
                (String) accessor
                        .getSessionAttributes()
                        .get("role");

        chatService.sendGroupMessage(
                userId,
                role,
                request.getContent()
        );
    }
}
