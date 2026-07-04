package com.autohub.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor
        implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    private final CustomUserDetailsService userDetailsService;

    private final OnlineUserStore onlineUserStore;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (StompCommand.CONNECT.equals(
                accessor.getCommand())) {

            String authHeader =
                    accessor.getFirstNativeHeader(
                            "Authorization"
                    );

            if(authHeader == null
                    ||
                    !authHeader.startsWith("Bearer ")) {

                throw new RuntimeException(
                        "Unauthorized"
                );
            }

            String token =
                    authHeader.substring(7);

            String username =
                    jwtUtil.extractUsername(token);

            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(
                                    username
                            );

            if(!jwtUtil.validateToken(
                    token,
                    username)) {

                throw new RuntimeException(
                        "Invalid token"
                );
            }

            Long userId =
                    jwtUtil.extractId(token);

            String role =
                    jwtUtil.extractRole(token);

            accessor.getSessionAttributes()
                    .put("userId", userId);

            accessor.getSessionAttributes()
                    .put("role", role);

            onlineUserStore.add(
                    role + "_" + userId
            );
        }

        return message;
    }
}