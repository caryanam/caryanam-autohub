package com.autohub.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor
        implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
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
        System.out.println(
                "COMMAND = " + accessor.getCommand()
        );

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            System.out.println("CONNECT RECEIVED");

            String authHeader =
                    accessor.getFirstNativeHeader(
                            "Authorization"
                    );

            System.out.println(
                    "AUTH HEADER = " + authHeader
            );

            if (authHeader == null
                    || !authHeader.startsWith("Bearer ")) {

                throw new RuntimeException(
                        "Unauthorized"
                );
            }

            String token =
                    authHeader.substring(7);

            System.out.println(
                    "TOKEN = " + token
            );

            String username =
                    jwtUtil.extractUsername(token);

            System.out.println(
                    "USERNAME = " + username
            );

            UserDetails userDetails =
                    customUserDetailsService
                            .loadUserByUsername(username);

            if (!jwtUtil.validateToken(
                    token,
                    userDetails.getUsername()
            )) {

                throw new RuntimeException(
                        "Invalid Token"
                );
            }

            Long userId =
                    jwtUtil.extractId(token);

            String role =
                    jwtUtil.extractRole(token);

            System.out.println(
                    "USER ID = " + userId
            );

            System.out.println(
                    "ROLE = " + role
            );

            accessor.getSessionAttributes()
                    .put("userId", userId);

            accessor.getSessionAttributes()
                    .put("role", role);

            // USER ONLINE MARK

            String userKey =
                    role + "_" + userId;

            onlineUserStore.add(userKey);

            System.out.println(
                    "ONLINE USER ADDED = "
                            + userKey
            );

            accessor.setUser(
                    new org.springframework.security.authentication
                            .UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            userDetails.getAuthorities()
                    )
            );
        }

        return message;
    }
}