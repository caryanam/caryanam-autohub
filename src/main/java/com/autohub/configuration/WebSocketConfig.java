package com.autohub.configuration;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {


    private final AuthChannelInterceptor interceptor;

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(interceptor);
    }
    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {

        //registry.enableSimpleBroker("/queue");

        registry.enableSimpleBroker(
                "/queue",
                "/topic"
        );

        registry.setApplicationDestinationPrefixes(
                "/app"
        );
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

}

