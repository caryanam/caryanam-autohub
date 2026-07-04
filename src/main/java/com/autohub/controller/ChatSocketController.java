package com.autohub.controller;

import com.autohub.dto.ChatMessageRequest;
import com.autohub.dto.TypingDTO;
import com.autohub.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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

        // GROUP CHAT

        if(Boolean.TRUE.equals(
                request.getGroupMessage())) {

            chatService.sendGroupMessage(
                    userId,
                    role,
                    request
            );

            return;
        }

        // PRIVATE CHAT

        chatService.sendMessage(
                userId,
                role,
                request
        );
    }

    @MessageMapping("/chat.typing")
    public void typing(
            TypingDTO dto){

        messagingTemplate.convertAndSend(
                "/queue/"
                        + dto.getReceiverRole()
                        + "_"
                        + dto.getReceiverId()
                        + "_typing",
                dto
        );
    }
}




