package com.autohub.configuration;

import com.autohub.dto.FacebookProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class FacebookWebClientConfig {

    private final FacebookProperties properties;

    public FacebookWebClientConfig(FacebookProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void verifyConfig() {
        // Deliberately does not log appSecret / pageAccessToken.
        log.info("Facebook Page Id     : {}", properties.pageId());
        log.info("Facebook Graph Version: {}", properties.graphVersion());
    }

    @Bean(name = "facebookWebClient")
    public WebClient facebookWebClient() {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeoutMs())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.readTimeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.readTimeoutMs(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
