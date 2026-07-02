package com.autohub.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;


@Component
@RequiredArgsConstructor
public class WebSocketDisconnectListener {

    private final OnlineUserStore onlineUserStore;

    @EventListener
    public void handleDisconnect(
            SessionDisconnectEvent event) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(
                        event.getMessage()
                );

        if (accessor.getSessionAttributes() == null) {
            return;
        }

        Long userId =
                (Long) accessor
                        .getSessionAttributes()
                        .get("userId");

        String role =
                (String) accessor
                        .getSessionAttributes()
                        .get("role");

        if (userId != null && role != null) {

            onlineUserStore.remove(
                    role + "_" + userId
            );

            System.out.println(
                    role + "_" + userId + " OFFLINE"
            );
        }
    }
}